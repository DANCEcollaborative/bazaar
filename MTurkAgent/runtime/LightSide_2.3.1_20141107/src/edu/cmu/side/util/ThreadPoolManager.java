package edu.cmu.side.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolManager
{

	//TODO: consider moving this elsewhere.
	private static int availableCores = Math.max(1, Runtime.getRuntime().availableProcessors()-1);
	private static ExecutorService sharedThreadPool = Executors.newFixedThreadPool(availableCores);
	
	public static synchronized int getThreadPoolSize()
	{
		return availableCores;
	}
	
	public static synchronized void setThreadPoolSize(int cores)
	{
		if(cores != availableCores)
		{
			System.out.println("Creating new thread pool for "+cores+" core(s).");
			availableCores = cores;
			sharedThreadPool = Executors.newFixedThreadPool(cores);
		}
	}

	public static synchronized ExecutorService getThreadPool()
	{
		return sharedThreadPool;
	}
}
