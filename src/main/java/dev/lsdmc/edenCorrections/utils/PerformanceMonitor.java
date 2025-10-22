package dev.lsdmc.edenCorrections.utils;

import org.bukkit.scheduler.BukkitRunnable;
import dev.lsdmc.edenCorrections.EdenCorrections;

import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;


public class PerformanceMonitor {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    
    private final AtomicLong lastTickTime = new AtomicLong(0);
    private final AtomicInteger slowTickCount = new AtomicInteger(0);
    private final AtomicInteger totalTickCount = new AtomicInteger(0);
    private final AtomicLong totalTickTime = new AtomicLong(0);
    
    
    private static final long SLOW_TICK_THRESHOLD_MS = 100; 
    private static final long CRITICAL_TICK_THRESHOLD_MS = 200; 
    private static final long SEVERE_TICK_THRESHOLD_MS = 500; 
    
    
    private long lastSlowTickLog = 0;
    private static final long SLOW_TICK_LOG_COOLDOWN_MS = 5000; 
    
    public PerformanceMonitor(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void initialize() {
        
        startPerformanceMonitoring();
    }
    
    
    private void startPerformanceMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                trackTickPerformance();
            }
        }.runTaskTimer(plugin, 0L, 1L); 
    }
    
    
    public void trackTickPerformance() {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastTickTime.getAndSet(currentTime);
        
        if (lastTime > 0) {
            long tickDuration = currentTime - lastTime;
            totalTickCount.incrementAndGet();
            totalTickTime.addAndGet(tickDuration);
            
            
            boolean canLog = (currentTime - lastSlowTickLog) > SLOW_TICK_LOG_COOLDOWN_MS;
            
            
            if (tickDuration > SLOW_TICK_THRESHOLD_MS) {
                int slowTicks = slowTickCount.incrementAndGet();
                
                if (tickDuration > SEVERE_TICK_THRESHOLD_MS) {
                    
                    logger.severe("SEVERE PERFORMANCE ISSUE: Tick took " + tickDuration + "ms (tick #" + totalTickCount.get() + ")");
                    lastSlowTickLog = currentTime;
                } else if (tickDuration > CRITICAL_TICK_THRESHOLD_MS) {
                    
                    if (canLog) {
                        logger.warning("CRITICAL: Very slow tick detected: " + tickDuration + "ms (tick #" + totalTickCount.get() + ")");
                        lastSlowTickLog = currentTime;
                    }
                } else if (tickDuration > SLOW_TICK_THRESHOLD_MS) {
                    
                    if (canLog && slowTicks % 50 == 0) { 
                        logger.info("PERFORMANCE: Slow tick detected: " + tickDuration + "ms (slow tick #" + slowTicks + ")");
                        lastSlowTickLog = currentTime;
                    }
                }
            }
        }
    }
    
    
    public PerformanceStats getPerformanceStats() {
        int totalTicks = totalTickCount.get();
        long totalTime = totalTickTime.get();
        int slowTicks = slowTickCount.get();
        
        double averageTickTime = totalTicks > 0 ? (double) totalTime / totalTicks : 0.0;
        double slowTickPercentage = totalTicks > 0 ? (double) slowTicks / totalTicks * 100 : 0.0;
        
        return new PerformanceStats(totalTicks, slowTicks, averageTickTime, slowTickPercentage);
    }
    
    
    public void resetCounters() {
        totalTickCount.set(0);
        slowTickCount.set(0);
        totalTickTime.set(0);
        lastTickTime.set(0);
        lastSlowTickLog = 0;
    }
    
    
    public void logPerformanceSummary() {
        PerformanceStats stats = getPerformanceStats();
        logger.info("=== PERFORMANCE SUMMARY ===");
        logger.info("Total ticks monitored: " + stats.getTotalTicks());
        logger.info("Slow ticks (>100ms): " + stats.getSlowTicks() + " (" + String.format("%.1f", stats.getSlowTickPercentage()) + "%)");
        logger.info("Average tick time: " + String.format("%.2f", stats.getAverageTickTime()) + "ms");
        
        
        if (stats.getSlowTickPercentage() > 10.0) { 
            logger.warning("PERFORMANCE WARNING: High percentage of slow ticks detected!");
        } else if (stats.getAverageTickTime() > 20.0) { 
            logger.warning("PERFORMANCE WARNING: High average tick time detected!");
        }
    }
    
    
    public static class PerformanceStats {
        private final int totalTicks;
        private final int slowTicks;
        private final double averageTickTime;
        private final double slowTickPercentage;
        
        public PerformanceStats(int totalTicks, int slowTicks, double averageTickTime, double slowTickPercentage) {
            this.totalTicks = totalTicks;
            this.slowTicks = slowTicks;
            this.averageTickTime = averageTickTime;
            this.slowTickPercentage = slowTickPercentage;
        }
        
        public int getTotalTicks() { return totalTicks; }
        public int getSlowTicks() { return slowTicks; }
        public double getAverageTickTime() { return averageTickTime; }
        public double getSlowTickPercentage() { return slowTickPercentage; }
    }
}