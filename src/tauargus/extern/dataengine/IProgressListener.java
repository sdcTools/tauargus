/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package tauargus.extern.dataengine;

public class IProgressListener {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected IProgressListener(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(IProgressListener obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  @SuppressWarnings("deprecation")
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        TauArgusJavaJNI.delete_IProgressListener(swigCPtr);
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
    TauArgusJavaJNI.IProgressListener_change_ownership(this, swigCPtr, false);
  }

  public void swigTakeOwnership() {
    swigCMemOwn = true;
    TauArgusJavaJNI.IProgressListener_change_ownership(this, swigCPtr, true);
  }

  public void UpdateProgress(int Perc) {
    TauArgusJavaJNI.IProgressListener_UpdateProgress(swigCPtr, this, Perc);
  }

  public IProgressListener() {
    this(TauArgusJavaJNI.new_IProgressListener(), true);
    TauArgusJavaJNI.IProgressListener_director_connect(this, swigCPtr, true, true);
  }

}
