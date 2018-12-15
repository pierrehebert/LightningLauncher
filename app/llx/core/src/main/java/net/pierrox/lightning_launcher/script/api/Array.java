package net.pierrox.lightning_launcher.script.api;

/**
 * The array is a collection of object.
 * @deprecated This collection isn't needed anymore and is kept for compatibility. Use plain JavaScript arrays instead.
 */
public class Array {
	private Object[] mArray;
	
	/*package*/ Array(Object[] array) {
		mArray = array;
	}
	
//	/*package*/ Array(List<? extends Object> array) {
//		mArray = array.toArray();
//	}
	
	/**
	 * Returns the number of objects in this array.
	 */
	public int getLength() {
		return mArray.length;
	}
	
	/**
	 * Returns the object at the specified index.
	 * @param index should be between 0 and length-1
	 * @return an object or undefined if out of bounds
	 */
	public Object getAt(int index) {
		if(index<0 || index >= mArray.length) {
			return null;
		} else {
			return mArray[index];
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<mArray.length; i++) {
			if(i>0) {
				sb.append(',');
			}
			sb.append(mArray[i].toString());
		}
		return sb.toString();
	}
}
