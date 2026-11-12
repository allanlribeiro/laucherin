// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;
import java.util.*;
/** Shuffle visits each remaining queue entry once. The saved queue itself is never reordered. */
final class LauncherinShuffleOrder {
 private int size=-1;private final List<Integer> remaining=new ArrayList<>();private final ArrayDeque<Integer> history=new ArrayDeque<>();
 void reset(){size=-1;remaining.clear();history.clear();}
 private void fill(int current,int count,Random random){remaining.clear();for(int i=0;i<count;i++)if(i!=current)remaining.add(i);Collections.shuffle(remaining,random);}
 int next(int current,int count,boolean repeatAll,Random random){if(count<=0)return -1;if(size!=count){history.clear();size=count;fill(current,count,random);}if(remaining.isEmpty()){if(!repeatAll)return -1;if(count==1)return current;fill(current,count,random);}history.push(current);if(history.size()>100)history.removeLast();return remaining.remove(0);}
 int previous(int current,int count){if(size!=count||history.isEmpty())return -1;remaining.remove(Integer.valueOf(current));remaining.add(0,current);return history.pop();}
}
