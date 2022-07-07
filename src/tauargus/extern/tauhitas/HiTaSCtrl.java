/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.1
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package tauargus.extern.tauhitas;

public class HiTaSCtrl {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected HiTaSCtrl(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(HiTaSCtrl obj) {
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
        tauhitasJNI.delete_HiTaSCtrl(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public HiTaSCtrl() {
    this(tauhitasJNI.new_HiTaSCtrl(), true);
  }

  public void SetProgressListener(IProgressListener ProgressListener) {
    tauhitasJNI.HiTaSCtrl_SetProgressListener(swigCPtr, this, IProgressListener.getCPtr(ProgressListener), ProgressListener);
  }

  public void SetCallback(ICallback jCallback) {
    tauhitasJNI.HiTaSCtrl_SetCallback(swigCPtr, this, ICallback.getCPtr(jCallback), jCallback);
  }

  public void SetJJconstantsInt(int ConstName, int ConstValue) {
    tauhitasJNI.HiTaSCtrl_SetJJconstantsInt(swigCPtr, this, ConstName, ConstValue);
  }

  public void SetJJconstantsDbl(int ConstName, double ConstValue) {
    tauhitasJNI.HiTaSCtrl_SetJJconstantsDbl(swigCPtr, this, ConstName, ConstValue);
  }

  public int GetJJconstantsInt(int ConstName) {
    return tauhitasJNI.HiTaSCtrl_GetJJconstantsInt(swigCPtr, this, ConstName);
  }

  public double GetJJconstantsDbl(int ConstName) {
    return tauhitasJNI.HiTaSCtrl_GetJJconstantsDbl(swigCPtr, this, ConstName);
  }

  public String GetErrorString(int ErrorNumber) {
    return tauhitasJNI.HiTaSCtrl_GetErrorString(swigCPtr, this, ErrorNumber);
  }

  public String GetVersion() {
    return tauhitasJNI.HiTaSCtrl_GetVersion(swigCPtr, this);
  }

  public void SetDebugMode(boolean debug) {
    tauhitasJNI.HiTaSCtrl_SetDebugMode(swigCPtr, this, debug);
  }

  public int FullJJ(String InFileJJ, String OutFile, int MaxTime, String ILMFile, String OutDir, String Solver) {
    return tauhitasJNI.HiTaSCtrl_FullJJ(swigCPtr, this, InFileJJ, OutFile, MaxTime, ILMFile, OutDir, Solver);
  }

  public int AHiTaS(String ParsFile, String FilesFile, int MaxTime, String ILMFile, String TauOutDir, String Solver, boolean DoSingleWithSingle, boolean DoSingleWithMore, boolean DoCountBounds) {
    return tauhitasJNI.HiTaSCtrl_AHiTaS(swigCPtr, this, ParsFile, FilesFile, MaxTime, ILMFile, TauOutDir, Solver, DoSingleWithSingle, DoSingleWithMore, DoCountBounds);
  }

}
