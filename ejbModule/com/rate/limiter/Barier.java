package com.rate.limiter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Session Bean implementation class Barier
 */

public class Barier {
	private int BarierLimit=0;
	private int QueueLimit=0;
	private volatile  AtomicInteger Enqueued=new AtomicInteger(0);
	private BlockingQueue<Integer> EnqueuedTocken=null;
	private BlockingQueue<Integer> ActiveTocken=null;
	private volatile  AtomicInteger Aviable=new AtomicInteger(0);
	private Logger log=Logger.getLogger("SingletonBarier");
	private final ScheduledExecutorService scheduler =Executors.newScheduledThreadPool(1);
	private int Frequency=1000;
	private int RateLimitPerc=0;
	private AtomicBoolean  isRaeteLimitingNeeded=new AtomicBoolean(false);
	
	/**
     * Default constructor. 
     */
	void init() {
		log.info("Setting Barier to "+10);
		setBarierLimit(2);
		Aviable.set(BarierLimit);
		
	}
    public Barier() {
        // TODO Auto-generated constructor stub
    }
      
	public int getBarierLimit() {
		return BarierLimit;
	}
	public void setBarierLimit(int BarierLimit) {
		this.BarierLimit = BarierLimit;
	}
	public void setBarierLimit(String BarierLimit) {
		this.BarierLimit = Integer.parseInt(BarierLimit);
		this.Aviable.set(this.BarierLimit);
		log.info("Setting Barier to "+BarierLimit);
	}
	
	public RLResult GetAccess() {
		boolean result=false;
		RLResult RLR=new RLResult();
		Integer EnqueueToken=new Integer(1);
		if (isRaeteLimitingNeeded.get()==false) {
			RLR.setRL(false);
			RLR.setRLResult(true);
			return RLR;
		}
		// Try To enueue request
		while(!result) {
			try {
				log.info("Enqueueing request");
				result=EnqueuedTocken.offer(EnqueueToken, 10, TimeUnit.MILLISECONDS);
				if(result==false) {
					log.warning("Enqueueing failed");
					break;
				} else {
					log.info("Enqueued, queue size: "+EnqueuedTocken.size());
				}
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				continue;
			}
		}
		
		//Data In queue now Activating int
		if(result==true) {
			// Wait for possibility to activate
			result=false;
			while(!result) {
				try {
					Integer x=ActiveTocken.poll(10, TimeUnit.MILLISECONDS);
					if(x!=null) {
						log.info("Succed to get Token size"+ActiveTocken.size());
						result=true;
					} else {
						log.warning("Failed to get Token size"+ActiveTocken.size());
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					result=false;
				}
			}
			// Activated
			EnqueuedTocken.poll();
			log.info("Freening Queue");
		}
			
		RLR.setRL(true);
		RLR.setRLResult(result);	
		return RLR;			
	}
	
	public void ReleaseAccess(RLResult wasRL) {
		boolean res= false;
		Integer val = new Integer(1);
		if(wasRL.isRL()) {
			log.info("Releasing Queue size: "+ActiveTocken.size());
			//while(!res) {		
					res=ActiveTocken.offer(val);
			//}
			log.info("Released with values:"+res+" Queue size: "+ActiveTocken.size());
		}
	}
	public int getQueueLimit() {
		return QueueLimit;
	}
	public void setQueueLimit(int queueLimit) {
		QueueLimit = queueLimit;
	}

	public void setQueueLimit(String queueLimit) {
		QueueLimit = Integer.parseInt(queueLimit);
		Enqueued.set(QueueLimit);
	}
	public BlockingQueue<Integer> getEnqueuedTocken() {
		return EnqueuedTocken;
	}
	public void setEnqueuedTocken(BlockingQueue<Integer> enqueuedTocken) {
		EnqueuedTocken = enqueuedTocken;
	}
	public BlockingQueue<Integer> getActiveTocken() {
		return ActiveTocken;
	}
	public void setActiveTocken(BlockingQueue<Integer> activeTocken) {
		ActiveTocken = activeTocken;
	}
	
	public void setActiveTocken(String Size) {
		ActiveTocken = new LinkedBlockingQueue<Integer>(Integer.parseInt(Size));
		log.info("Setting ActiveSize to "+Size);
		for(int Count=0;Count<Integer.parseInt(Size); Count++) {
			try {
				ActiveTocken.put(new Integer(Count));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void setEnqueuedTocken(String Size) {
		log.info("Setting Queue size to "+Size);
		EnqueuedTocken = new LinkedBlockingQueue<Integer>(Integer.parseInt(Size));
	}
	public int getFrequency() {
		return Frequency;
	}
	public void setFrequency(int frequency) {
		Frequency = frequency;
	}
	public void setFrequency(String frequency) {
		Frequency = Integer.parseInt(frequency);
		final Runnable task = new Runnable() {
			//float MaxSize=((float)RateLimitPerc/100)*(float)Runtime.getRuntime().maxMemory();
			public void run() { log.fine("Checking");
			log.fine("Heap Size: "+Runtime.getRuntime().maxMemory());
			log.fine("Free Haep Size: "+Runtime.getRuntime().freeMemory());
			log.fine("Total Haep Size: "+Runtime.getRuntime().totalMemory());
			long freeMemory=Runtime.getRuntime().freeMemory();
			long TotalMemory=Runtime.getRuntime().totalMemory();
			long MaxMemeory=Runtime.getRuntime().maxMemory();
			float Perc= 100*((float)freeMemory/TotalMemory)+100*((float)TotalMemory/MaxMemeory);
			if (TotalMemory-freeMemory>((float)RateLimitPerc/100)*MaxMemeory) {
				setRaeteLimitingNeeded(true);
				log.fine("Rate Limiting needed\n"+"\tTotal-free: "+(TotalMemory-freeMemory)+"\n"+"\tMaxSize: "+((float)RateLimitPerc/100)*MaxMemeory);
			} else {
				setRaeteLimitingNeeded(false);
			}
			log.fine("Perc Free Heap  on total memory: "+Perc);
			}
		};
		//final ScheduledFuture<?> tsk = scheduler.scheduleAtFixedRate(task, 10000, Frequency, TimeUnit.MILLISECONDS);
		scheduler.scheduleWithFixedDelay(task, 10000, Frequency, TimeUnit.MILLISECONDS);
		
	}
	public int getRateLimitPerc() {
		return RateLimitPerc;
	}
	public void setRateLimitPerc(int rateLimitPerc) {
		RateLimitPerc = rateLimitPerc;
	}
	public void setRateLimitPerc(String rateLimitPerc) {
		RateLimitPerc = Integer.parseInt(rateLimitPerc);
	}
	public boolean isRaeteLimitingNeeded() {
		return isRaeteLimitingNeeded.get();
	}
	public void setRaeteLimitingNeeded(boolean isRaeteLimitingNeeded) {
		this.isRaeteLimitingNeeded.set(isRaeteLimitingNeeded);
	}
}
