/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.1
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package tauargus.extern.tauhitas;

public class tauhitasJNI {
  public final static native void delete_IProgressListener(long jarg1);
  public final static native void IProgressListener_UpdateLB(long jarg1, IProgressListener jarg1_, int jarg2);
  public final static native void IProgressListener_UpdateUB(long jarg1, IProgressListener jarg1_, int jarg2);
  public final static native void IProgressListener_UpdateGroups(long jarg1, IProgressListener jarg1_, int jarg2);
  public final static native void IProgressListener_UpdateTables(long jarg1, IProgressListener jarg1_, int jarg2);
  public final static native void IProgressListener_UpdateDiscrepancy(long jarg1, IProgressListener jarg1_, double jarg2);
  public final static native void IProgressListener_UpdateTime(long jarg1, IProgressListener jarg1_, int jarg2);
  public final static native void IProgressListener_UpdateNSuppressed(long jarg1, IProgressListener jarg1_, int jarg2);
  public final static native long new_IProgressListener();
  public final static native void IProgressListener_director_connect(IProgressListener obj, long cptr, boolean mem_own, boolean weak_global);
  public final static native void IProgressListener_change_ownership(IProgressListener obj, long cptr, boolean take_or_release);
  public final static native void delete_ICallback(long jarg1);
  public final static native int ICallback_SetStopTime(long jarg1, ICallback jarg1_);
  public final static native long new_ICallback();
  public final static native void ICallback_director_connect(ICallback obj, long cptr, boolean mem_own, boolean weak_global);
  public final static native void ICallback_change_ownership(ICallback obj, long cptr, boolean take_or_release);
  public final static native long new_HiTaSCtrl();
  public final static native void delete_HiTaSCtrl(long jarg1);
  public final static native void HiTaSCtrl_SetProgressListener(long jarg1, HiTaSCtrl jarg1_, long jarg2, IProgressListener jarg2_);
  public final static native void HiTaSCtrl_SetCallback(long jarg1, HiTaSCtrl jarg1_, long jarg2, ICallback jarg2_);
  public final static native void HiTaSCtrl_SetJJconstantsInt(long jarg1, HiTaSCtrl jarg1_, int jarg2, int jarg3);
  public final static native void HiTaSCtrl_SetJJconstantsDbl(long jarg1, HiTaSCtrl jarg1_, int jarg2, double jarg3);
  public final static native int HiTaSCtrl_GetJJconstantsInt(long jarg1, HiTaSCtrl jarg1_, int jarg2);
  public final static native double HiTaSCtrl_GetJJconstantsDbl(long jarg1, HiTaSCtrl jarg1_, int jarg2);
  public final static native String HiTaSCtrl_GetErrorString(long jarg1, HiTaSCtrl jarg1_, int jarg2);
  public final static native String HiTaSCtrl_GetVersion(long jarg1, HiTaSCtrl jarg1_);
  public final static native void HiTaSCtrl_SetDebugMode(long jarg1, HiTaSCtrl jarg1_, boolean jarg2);
  public final static native int HiTaSCtrl_FullJJ(long jarg1, HiTaSCtrl jarg1_, String jarg2, String jarg3, int jarg4, String jarg5, String jarg6, String jarg7);
  public final static native int HiTaSCtrl_AHiTaS(long jarg1, HiTaSCtrl jarg1_, String jarg2, String jarg3, int jarg4, String jarg5, String jarg6, String jarg7, boolean jarg8, boolean jarg9, boolean jarg10);

  public static void SwigDirector_IProgressListener_UpdateLB(IProgressListener jself, int Perc) {
    jself.UpdateLB(Perc);
  }
  public static void SwigDirector_IProgressListener_UpdateUB(IProgressListener jself, int Perc) {
    jself.UpdateUB(Perc);
  }
  public static void SwigDirector_IProgressListener_UpdateGroups(IProgressListener jself, int Perc) {
    jself.UpdateGroups(Perc);
  }
  public static void SwigDirector_IProgressListener_UpdateTables(IProgressListener jself, int Perc) {
    jself.UpdateTables(Perc);
  }
  public static void SwigDirector_IProgressListener_UpdateDiscrepancy(IProgressListener jself, double value) {
    jself.UpdateDiscrepancy(value);
  }
  public static void SwigDirector_IProgressListener_UpdateTime(IProgressListener jself, int value) {
    jself.UpdateTime(value);
  }
  public static void SwigDirector_IProgressListener_UpdateNSuppressed(IProgressListener jself, int value) {
    jself.UpdateNSuppressed(value);
  }
  public static int SwigDirector_ICallback_SetStopTime(ICallback jself) {
    return jself.SetStopTime();
  }

  private final static native void swig_module_init();
  static {
    swig_module_init();
  }
}
