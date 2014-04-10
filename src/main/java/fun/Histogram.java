package fun;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Histogram {
    private final Map<Integer, Integer> dataPoints = new ConcurrentHashMap<>();
    private int minPoint = Integer.MAX_VALUE;
    private int maxPoint = Integer.MIN_VALUE;
    private int maxValue = Integer.MIN_VALUE;
    private int maxPosition = -1;
    private long totalBumps = 0;

    public Histogram bucketed(Function<Integer,Integer> bucketing) {
        Histogram b = new Histogram();
        dataPoints.forEach((k,v) ->{
            int bk = bucketing.apply(k);
            b.incrementBy(bk, v);
        });
        return b;
    }

    public int increment(int point) {
        return incrementBy(point, 1);
    }

    private int incrementBy(int point, int amount) {
        final Integer value = dataPoints.get(point);
        final int newValue = (value != null) ? (value + amount) : amount;
        dataPoints.put(point, newValue);
        if (point < minPoint) {
            minPoint = point;
        }
        if (point > maxPoint) {
            maxPoint = point;
        }
        if (newValue > maxValue) {
            maxValue = newValue;
            maxPosition = point;
        }
        totalBumps+=amount;
        return newValue;
    }

    public int min() {
        return minPoint;
    }
    public int max() {
        return maxPoint;
    }
    public int height() {
        return maxValue;
    }
    public int peakPosition() {
        return maxPosition;
    }
    public long bumps() {
        return totalBumps;
    }

    public int valueAt(int x) {
        return dataPoints.getOrDefault(x, 0);
    }
}
