/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.10
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package tauargus.extern.taurounder;

public class RCallback {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected RCallback(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(RCallback obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        taurounderJNI.delete_RCallback(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  protected void swigDirectorDisconnect() {
    swigCMemOwn = false;
    delete();
  }

  public void swigReleaseOwnership() {
    swigCMemOwn = false;
    taurounderJNI.RCallback_change_ownership(this, swigCPtr, false);
  }

  public void swigTakeOwnership() {
    swigCMemOwn = true;
    taurounderJNI.RCallback_change_ownership(this, swigCPtr, true);
  }

  public int SetExtraTime() {
    return taurounderJNI.RCallback_SetExtraTime(swigCPtr, this);
  }

  public RCallback() {
    this(taurounderJNI.new_RCallback(), true);
    taurounderJNI.RCallback_director_connect(this, swigCPtr, swigCMemOwn, true);
  }

}
