package edu.cmu.side.model;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class FreqMap<E> extends TreeMap<E, Integer> implements Iterable{

	public void count(E e){
		if(this.containsKey(e)){
			Integer i = this.get(e);
			this.put(e, i+1);
		}else{
			this.put(e, 1);
		}
	}

	public Integer safeGet(E key){
		if(containsKey(key)){
			return get(key);
		}else return 0;
	}

	public ArrayList<E[]> convertToArrayList(){
		ArrayList<E[]> out = new ArrayList<E[]>();
		for(E entry : this.keySet()){
			for(int i = 0; i < this.get(entry); i++){
				out.add(((E[])(new String[]{(String)entry})));
			}
		}
		return out;
	}

	public Integer sum(){
		int count = 0;
		for(E e : this.keySet()){
			count += this.get(e);
		}
		return count;
	}

	public void count(E e, Integer i){
		this.put(e, i);
	}

	public void countAll(Collection<E> c){
		for(E e : c){
			count(e);
		}
	}

	public List<E> top(int n){
		List<E> top = new ArrayList<E>();
		int index = 0;
		Iterator itr = iterator();
		while(index < n && itr.hasNext()){
			top.add((E)itr.next());
			index++;
		}
		return top;
	}
	@Override
	public Iterator iterator() {
		ArrayList<FreqUnigram> unis = new ArrayList<FreqUnigram>();
		for(E uni : this.keySet()){
			unis.add(new FreqUnigram(uni, this.get(uni)));
		}
		Collections.sort(unis);
		ArrayList<E> out = new ArrayList<E>();
		for(FreqUnigram e : unis){
			out.add(e.uni);
		}
		return out.iterator();
	}
	private class FreqUnigram implements Comparable<FreqUnigram>{
		public Integer count;
		public E uni;
		public FreqUnigram(E inU, int inC){
			uni = inU;
			count = inC;
		}
		@Override
		public int compareTo(FreqUnigram o) {
			return -count.compareTo(o.count);
		}

	}
	public E getMaxKey()
	{
		E maxKey = null;
		int maxCount = 0;
		for(E key : keySet())
		{
			if(get(key) > maxCount)
			{
				maxCount = get(key);
				maxKey = key;
			}
		}
		return maxKey;
	}

}

