package edu.cmu.side.plugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import edu.cmu.side.model.OrderedPluginMap;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.PredictionResult;
import edu.cmu.side.util.ThreadPoolManager;
import edu.cmu.side.view.util.ParallelTaskUpdater;
import edu.cmu.side.view.util.ParallelTaskUpdater.Completion;

public abstract class ParallelLearningPlugin extends LearningPlugin
{
	//static ExecutorService LEARNER_THREAD_POOL = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()-1));
	
	private static final long serialVersionUID = 1L;

	public List<PredictionResult> doCrossValidation(final FeatureTable table, final Map<Integer, Integer> foldsMap, final Set<Integer> folds, final OrderedPluginMap wrappers,
			final StatusUpdater progressIndicator) throws Exception
	{
		final int numFolds = folds.size();
		
		List<Callable<PredictionResult>> tasks = new ArrayList<Callable<PredictionResult>>();

		
//		final ArrayList<Double> times = new ArrayList<Double>(numFolds);
		List<PredictionResult> results = new ArrayList<PredictionResult>();

		final Map<String, String> learnerSettings = ParallelLearningPlugin.this.generateConfigurationSettings();
		

		if(updater instanceof ParallelTaskUpdater)
		{
			((ParallelTaskUpdater)updater).setTasks(numFolds);
		}
		
		
		for(final int fold : folds)
		{
			Callable<PredictionResult> task = new Callable<PredictionResult>()
			{

				@Override
				public PredictionResult call() throws Exception
				{
					if(halt)
					{
						return new PredictionResult(new ArrayList<String>());
					}
					
					// clone the learner with its current settings
					LearningPlugin clonedLearner;
					try
					{
						clonedLearner = ParallelLearningPlugin.this.clone();
					}
					catch (Exception e)
					{
						logger.warning("ParallelLearningPlugin 52:\tError cloning learner "+ParallelLearningPlugin.this+". Continuing with un-cloned learner");
						e.printStackTrace();
						clonedLearner = ParallelLearningPlugin.this;
					}
					
					//clone the *wrappers* too!
					final OrderedPluginMap clonedWrappers = new OrderedPluginMap();
					
					for(SIDEPlugin key : wrappers.keySet())
					{
						SIDEPlugin clone = key.getClass().newInstance();
						
						clonedWrappers.put(clone, wrappers.get(key));
					}

					if(updater instanceof ParallelTaskUpdater)
					{
						((ParallelTaskUpdater)updater).updateCompletion("Starting fold", fold, Completion.STARTED);
					}
					
					
					logger.finest("ParallelLearningPlugin 57:\tstarting to validate fold "+fold);
					PredictionResult result = clonedLearner.validateFold(fold, table, foldsMap, numFolds, clonedWrappers, progressIndicator);
					logger.finest("ParallelLearningPlugin 59:\tdone validating fold "+fold);
					
					if(updater instanceof ParallelTaskUpdater)
					{
						((ParallelTaskUpdater)updater).updateCompletion("Finished fold", fold, Completion.DONE);
					} 
					
					return result;
				}
				
			};
			tasks.add(task);
			if(updater instanceof ParallelTaskUpdater)
			{
				((ParallelTaskUpdater)updater).updateCompletion("Queueing fold", fold, Completion.WAITING);
			}
		}
		

		List<Future<PredictionResult>> futureResults = ThreadPoolManager.getThreadPool().invokeAll(tasks);
		
		for(Future<PredictionResult> future : futureResults)
		{
			results.add(future.get());
		}
		
		return results;
	}

	/*@Override
	public FeatureTable wrapTableBefore(FeatureTable newData, int fold, Map<Integer, Integer> foldsMap, StatusUpdater progressIndicator,
			OrderedPluginMap wrappers, boolean learn)
	{
		for (SIDEPlugin wrapper : wrappers.keySet())
		{
			synchronized(wrapper)
			{
				SIDEPlugin clone;
				try
				{
					clone = wrapper.getClass().newInstance();
				}
				catch (Exception e)
				{
					logger.warn("ParallelLearningPlugin 91:\tError cloning wrapper "+wrapper+". Continuing with un-cloned wrapper");
					e.printStackTrace();
					clone = wrapper;
				}
				clone.configureFromSettings(wrappers.get(wrapper));
				if (learn)
				{
					((WrapperPlugin) clone).learnFromTrainingData(newData, fold, foldsMap, progressIndicator);
					//whatever the wrapper has learned is stored in configuration settings.
					wrappers.put(wrapper, clone.generateConfigurationSettings());
				}
			
				newData = ((WrapperPlugin) clone).wrapTableBefore(newData, fold, foldsMap, progressIndicator);
			}
		}
		return newData;
	}*/
	
}
