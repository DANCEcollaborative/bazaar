package edu.cmu.side.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import edu.cmu.side.Workbench;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.util.ThreadPoolManager;
import edu.cmu.side.view.util.ParallelTaskUpdater;
import edu.cmu.side.view.util.ParallelTaskUpdater.Completion;

public abstract class ParallelFeaturePlugin extends FeaturePlugin
{

	//final static ExecutorService EXTRACTOR_THREAD_POOL = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()-1));
	/**
	 * 
	 * @param documents in a corpus
	 * @return All features that this plugin should extract from each document in this corpus.
	 */
	@Override
	public Collection<FeatureHit> extractFeatureHits(DocumentList documents, Map<String, String> configuration, StatusUpdater update)
	{
		halt = false;
		this.configureFromSettings(configuration);
		return extractFeatureHitsForSubclass(documents, update);
	}

	@Override
	public Collection<FeatureHit> extractFeatureHitsForSubclass(final DocumentList documents, final StatusUpdater updater)
	{
		final int numTasks = Math.min(documents.getSize(), Runtime.getRuntime().availableProcessors());
		final HashSet<FeatureHit> allHits = new HashSet<FeatureHit>();
		
		long start = System.currentTimeMillis();
		
		Collection<Callable<Collection<FeatureHit>>> tasks = new ArrayList<Callable<Collection<FeatureHit>>>();

		if(updater instanceof ParallelTaskUpdater)
		{
			((ParallelTaskUpdater)updater).setTasks(numTasks);
		}
		updater.update("Starting Extraction", 0, documents.getSize());
		
		final int size = documents.getSize();
		final int chunk = size/numTasks;
		for(int t = 0; t < numTasks; t++)
		{ 
			final int threadIndex = t;
			Callable<Collection<FeatureHit>> extraction = new Callable<Collection<FeatureHit>>()
			{
				final ArrayList<FeatureHit> hits = new ArrayList<FeatureHit>();
				
				@Override
				public Collection<FeatureHit> call()
				{
					String pluginName = ParallelFeaturePlugin.this.toString();
					
					if(updater instanceof ParallelTaskUpdater)
					{
						((ParallelTaskUpdater)updater).updateCompletion("Starting Extraction Thread", threadIndex, Completion.STARTED);
					}
					
					for(int index = threadIndex; index < size; index += numTasks)
					//for(int index = offset; index < size && index < offset + chunk; index++)
					{
						if(halt)
						{
							return hits;
						}
						
						//if(index % maxThreads == threadIndex)
						{
							if((index+1)%50 == 0 || index == size)
							{
								synchronized(updater)
								{updater.update("Extracting " + pluginName, index+1, size);}
							}
							
							if(index/numTasks == chunk/2)
							{
								if(updater instanceof ParallelTaskUpdater)
								{
									((ParallelTaskUpdater)updater).updateCompletion("Making Progress on Thread", threadIndex, Completion.PROGRESS);
								}
								
							}
							
							
							Collection<FeatureHit> docHits = extractFeatureHitsFromDocument(documents, index);
							hits.addAll(docHits);
						}
					}
					logger.fine("Thread "+threadIndex + " complete.");
					if(updater instanceof ParallelTaskUpdater)
					{
						((ParallelTaskUpdater)updater).updateCompletion("Finished Extraction Thread", threadIndex, Completion.DONE);
					}
					return hits;
				}
			};
			tasks.add(extraction);
			if(updater instanceof ParallelTaskUpdater)
			{
				((ParallelTaskUpdater)updater).updateCompletion("Queued Extraction Thread", threadIndex, Completion.WAITING);
			}
		}


		logger.fine("invoking "+tasks.size()+" tasks...");
		try
		{
			ExecutorService pool = ThreadPoolManager.getThreadPool();
			List<Future<Collection<FeatureHit>>> results = pool.invokeAll(tasks);

			for(Future<Collection<FeatureHit>> result: results)
			{
				allHits.addAll(result.get());
			}
			
		}
		catch (Exception ex)
		{
			// TODO Auto-generated catch block
			ex.printStackTrace();
			
			throw new RuntimeException("Feature Extraction Failed: "+ex.getMessage(), ex);
			
		}
		
		logger.fine(String.format("Parallel extraction complete in %.1f seconds.\n",(System.currentTimeMillis()-start)/1000.0));
		
		updater.update(this+" Extraction complete.");
		
		return allHits;
	}
	
	public abstract Collection<FeatureHit> extractFeatureHitsFromDocument(DocumentList documents, int i);
	
}
