/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.1
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package tauargus.extern.taurounder;

public class RounderCtrl {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected RounderCtrl(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(RounderCtrl obj) {
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
        taurounderJNI.delete_RounderCtrl(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void SetProgressListener(RProgressListener jProgressListener) {
    taurounderJNI.RounderCtrl_SetProgressListener(swigCPtr, this, RProgressListener.getCPtr(jProgressListener), jProgressListener);
  }

  public void SetCallback(RCallback jCallback) {
    taurounderJNI.RounderCtrl_SetCallback(swigCPtr, this, RCallback.getCPtr(jCallback), jCallback);
  }

  public void SetDoubleConstant(int variable, double value) {
    taurounderJNI.RounderCtrl_SetDoubleConstant(swigCPtr, this, variable, value);
  }

  public int DoRound(String Solver, String InFileName, double Base, double[] UpperBound, double[] LowerBound, int Auditing, String SolutionFile, String StatisticsFile, String LicenseFile, String LogFile, int MaxTime, int ZeroRestricted, String NamePathExe, double[] MaxJump, int[] NumberJump, double[] UsedTime, int[] ErrorCode) {
    return taurounderJNI.RounderCtrl_DoRound(swigCPtr, this, Solver, InFileName, Base, UpperBound, LowerBound, Auditing, SolutionFile, StatisticsFile, LicenseFile, LogFile, MaxTime, ZeroRestricted, NamePathExe, MaxJump, NumberJump, UsedTime, ErrorCode);
  }

  public String GetVersion() {
    return taurounderJNI.RounderCtrl_GetVersion(swigCPtr, this);
  }

  public RounderCtrl() {
    this(taurounderJNI.new_RounderCtrl(), true);
  }

}
