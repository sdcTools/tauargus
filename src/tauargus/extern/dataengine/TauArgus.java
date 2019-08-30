/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.12
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package tauargus.extern.dataengine;

public class TauArgus {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected TauArgus(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(TauArgus obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        TauArgusJavaJNI.delete_TauArgus(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public TauArgus() {
    this(TauArgusJavaJNI.new_TauArgus(), true);
  }

  public void SetProgressListener(IProgressListener ProgressListener) {
    TauArgusJavaJNI.TauArgus_SetProgressListener(swigCPtr, this, IProgressListener.getCPtr(ProgressListener), ProgressListener);
  }

  public boolean SetAllNonStructuralAsEmpty(int TableIndex) {
    return TauArgusJavaJNI.TauArgus_SetAllNonStructuralAsEmpty(swigCPtr, this, TableIndex);
  }

  public boolean SetSingleNonStructuralAsEmpty(int TableIndex, int[] DimIndex) {
    return TauArgusJavaJNI.TauArgus_SetSingleNonStructuralAsEmpty(swigCPtr, this, TableIndex, DimIndex);
  }

  public boolean SetSingleEmptyAsNonStructural(int TableIndex, int[] DimIndex) {
    return TauArgusJavaJNI.TauArgus_SetSingleEmptyAsNonStructural(swigCPtr, this, TableIndex, DimIndex);
  }

  public boolean SetAllEmptyNonStructural(int TableIndex) {
    return TauArgusJavaJNI.TauArgus_SetAllEmptyNonStructural(swigCPtr, this, TableIndex);
  }

  public boolean SetSecondaryFromHierarchicalAMPL(String FileName, int TableIndex, int[] ErrorCode) {
    return TauArgusJavaJNI.TauArgus_SetSecondaryFromHierarchicalAMPL(swigCPtr, this, FileName, TableIndex, ErrorCode);
  }

  public boolean WriteHierarchicalTableInAMPLFormat(String AMPLFilename, String TempDir, int Tableindex, double MaxScale, int[] ErrorCode) {
    return TauArgusJavaJNI.TauArgus_WriteHierarchicalTableInAMPLFormat(swigCPtr, this, AMPLFilename, TempDir, Tableindex, MaxScale, ErrorCode);
  }

  public boolean SetRoundedResponse(String RoundedFile, int TableIndex) {
    return TauArgusJavaJNI.TauArgus_SetRoundedResponse(swigCPtr, this, RoundedFile, TableIndex);
  }

  public double MaximumProtectionLevel(int TableIndex) {
    return TauArgusJavaJNI.TauArgus_MaximumProtectionLevel(swigCPtr, this, TableIndex);
  }

  public boolean SetProtectionLevelsForFrequencyTable(int TableIndex, int Base, int K) {
    return TauArgusJavaJNI.TauArgus_SetProtectionLevelsForFrequencyTable(swigCPtr, this, TableIndex, Base, K);
  }

  public boolean SetTableCellCost(int TableIndex, int[] DimIndex, double Cost) {
    return TauArgusJavaJNI.TauArgus_SetTableCellCost(swigCPtr, this, TableIndex, DimIndex, Cost);
  }

  public boolean GetCellStatusStatistics(int TableIndex, int[] StatusFreq, int[] StatusCellFreq, int[] StatusHoldingFreq, double[] StatusCellResponse, double[] StatusCellCost) {
    return TauArgusJavaJNI.TauArgus_GetCellStatusStatistics(swigCPtr, this, TableIndex, StatusFreq, StatusCellFreq, StatusHoldingFreq, StatusCellResponse, StatusCellCost);
  }

  public boolean WriteTableInAMPLFormat(String AMPLFileName, int TableIndex) {
    return TauArgusJavaJNI.TauArgus_WriteTableInAMPLFormat(swigCPtr, this, AMPLFileName, TableIndex);
  }

  public void SetInFileInfo(boolean IsFixedFormat, String Seperator) {
    TauArgusJavaJNI.TauArgus_SetInFileInfo(swigCPtr, this, IsFixedFormat, Seperator);
  }

  public boolean ComputeCodesToIndices(int TableIndex, String[] sCode, int[] DimIndex) {
    return TauArgusJavaJNI.TauArgus_ComputeCodesToIndices(swigCPtr, this, TableIndex, sCode, DimIndex);
  }

  public int CheckRealizedLowerAndUpperValues(int TabNr) {
    return TauArgusJavaJNI.TauArgus_CheckRealizedLowerAndUpperValues(swigCPtr, this, TabNr);
  }

  public boolean SetCTAValues(int TabIndex, int CelNr, double OrgVal, double CTAVal, int[] Sec) {
    return TauArgusJavaJNI.TauArgus_SetCTAValues(swigCPtr, this, TabIndex, CelNr, OrgVal, CTAVal, Sec);
  }

  public boolean SetRealizedLowerAndUpper(int TabNr, int CelNr, double RealizedUpper, double RealizedLower) {
    return TauArgusJavaJNI.TauArgus_SetRealizedLowerAndUpper(swigCPtr, this, TabNr, CelNr, RealizedUpper, RealizedLower);
  }

  public boolean UndoSecondarySuppress(int TableIndex, int SortSuppress) {
    return TauArgusJavaJNI.TauArgus_UndoSecondarySuppress(swigCPtr, this, TableIndex, SortSuppress);
  }

  public boolean SetTableSafetyInfo(int TabIndex, boolean HasMaxScore, boolean DominanceRule, int[] DominanceNumber, int[] DominancePerc, boolean PQRule, int[] PriorPosteriorP, int[] PriorPosteriorQ, int[] PriorPosteriorN, boolean HasFreq, int CellFreqSafetyPerc, int SafeMinRec, boolean HasStatus, int ManualSafetyPerc, boolean ApplyZeroRule, double ZeroSafetyRange, boolean EmptyCellAsNonStructural, int NSEmptySafetyRange, int[] ErrorCode) {
    return TauArgusJavaJNI.TauArgus_SetTableSafetyInfo(swigCPtr, this, TabIndex, HasMaxScore, DominanceRule, DominanceNumber, DominancePerc, PQRule, PriorPosteriorP, PriorPosteriorQ, PriorPosteriorN, HasFreq, CellFreqSafetyPerc, SafeMinRec, HasStatus, ManualSafetyPerc, ApplyZeroRule, ZeroSafetyRange, EmptyCellAsNonStructural, NSEmptySafetyRange, ErrorCode);
  }

  public boolean SetVariableForTable(int Index, int nMissing, String Missing1, String Missing2, String TotalCode, int nDec, boolean IsPeeper, String PeeperCode, boolean IsHierarchical, boolean IsNumeriek, int nPos) {
    return TauArgusJavaJNI.TauArgus_SetVariableForTable(swigCPtr, this, Index, nMissing, Missing1, Missing2, TotalCode, nDec, IsPeeper, PeeperCode, IsHierarchical, IsNumeriek, nPos);
  }

  public boolean CompletedTable(int Index, int[] ErrorCode, String FileName, boolean CalculateTotals, boolean SetCalculatedTotalsAsSafe, boolean ForCoverTable) {
    return TauArgusJavaJNI.TauArgus_CompletedTable(swigCPtr, this, Index, ErrorCode, FileName, CalculateTotals, SetCalculatedTotalsAsSafe, ForCoverTable);
  }

  public boolean SetInTable(int Index, String[] sCode, double Shadow, double Cost, double Resp, int Freq, double[] MaxScoreCell, double[] MaxScoreHolding, int Status, double LowerProtectionLevel, double UpperProtectionLevel, int[] ErrorCode, int[] ErrVNum) {
    return TauArgusJavaJNI.TauArgus_SetInTable(swigCPtr, this, Index, sCode, Shadow, Cost, Resp, Freq, MaxScoreCell, MaxScoreHolding, Status, LowerProtectionLevel, UpperProtectionLevel, ErrorCode, ErrVNum);
  }

  public void ThroughTable() {
    TauArgusJavaJNI.TauArgus_ThroughTable(swigCPtr, this);
  }

  public boolean SetTotalsInCodeList(int NumberofVariables, int[] VarIndex, int[] ErrorCode, int[] ErrorInVarIndex) {
    return TauArgusJavaJNI.TauArgus_SetTotalsInCodeList(swigCPtr, this, NumberofVariables, VarIndex, ErrorCode, ErrorInVarIndex);
  }

  public boolean SetInCodeList(int NumberofVar, int[] VarIndex, String[] sCode, int[] ErrorCode, int[] ErrorInVarIndex) {
    return TauArgusJavaJNI.TauArgus_SetInCodeList(swigCPtr, this, NumberofVar, VarIndex, sCode, ErrorCode, ErrorInVarIndex);
  }

  public boolean WriteCellRecords(int TableIndex, String FileName, int SBS, boolean SBSLevel, boolean SuppressEmpty, String FirstLine, boolean ShowUnsafe, boolean EmbedQuotes, int RespType) {
    return TauArgusJavaJNI.TauArgus_WriteCellRecords(swigCPtr, this, TableIndex, FileName, SBS, SBSLevel, SuppressEmpty, FirstLine, ShowUnsafe, EmbedQuotes, RespType);
  }

  public void GetTotalTabelSize(int TableIndex, int[] nCell, int[] SizeDataCell) {
    TauArgusJavaJNI.TauArgus_GetTotalTabelSize(swigCPtr, this, TableIndex, nCell, SizeDataCell);
  }

  public int SetSecondaryJJFORMAT(int TableIndex, String FileName, boolean WithBogus, int[] nSetSecondary) {
    return TauArgusJavaJNI.TauArgus_SetSecondaryJJFORMAT(swigCPtr, this, TableIndex, FileName, WithBogus, nSetSecondary);
  }

  public boolean WriteJJFormat(int TableIndex, String FileName, double LowerBound, double UpperBound, boolean WithBogus, boolean AsPerc, boolean ForRounding) {
    return TauArgusJavaJNI.TauArgus_WriteJJFormat(swigCPtr, this, TableIndex, FileName, LowerBound, UpperBound, WithBogus, AsPerc, ForRounding);
  }

  public boolean WriteCSV(int TableIndex, String FileName, boolean EmbedQuotes, int[] DimSequence, int RespType) {
    return TauArgusJavaJNI.TauArgus_WriteCSV(swigCPtr, this, TableIndex, FileName, EmbedQuotes, DimSequence, RespType);
  }

  public boolean GetCellDistance(int TableIndex, int[] DimIndex, int[] Distance) {
    return TauArgusJavaJNI.TauArgus_GetCellDistance(swigCPtr, this, TableIndex, DimIndex, Distance);
  }

  public boolean PrepareCellDistance(int TableIndex) {
    return TauArgusJavaJNI.TauArgus_PrepareCellDistance(swigCPtr, this, TableIndex);
  }

  public int SetSecondaryGHMITER(String FileName, int TableIndex, int[] nSetSecondary, boolean IsSingleton) {
    return TauArgusJavaJNI.TauArgus_SetSecondaryGHMITER(swigCPtr, this, FileName, TableIndex, nSetSecondary, IsSingleton);
  }

  public int WriteGHMITERDataCell(String FileName, int TableIndex, boolean IsSingleton) {
    return TauArgusJavaJNI.TauArgus_WriteGHMITERDataCell(swigCPtr, this, FileName, TableIndex, IsSingleton);
  }

  public int WriteGHMITERSteuer(String FileName, String EndString1, String EndString2, int TableIndex) {
    return TauArgusJavaJNI.TauArgus_WriteGHMITERSteuer(swigCPtr, this, FileName, EndString1, EndString2, TableIndex);
  }

  public boolean GetVarCodeProperties(int VarIndex, int CodeIndex, int[] IsParent, int[] IsActive, int[] IsMissing, int[] Level, int[] nChildren, String[] Code) {
    return TauArgusJavaJNI.TauArgus_GetVarCodeProperties(swigCPtr, this, VarIndex, CodeIndex, IsParent, IsActive, IsMissing, Level, nChildren, Code);
  }

  public boolean UnsafeVariableCodes(int VarIndex, int CodeIndex, int[] IsMissing, int[] Freq, java.lang.String[] Code, int[] Count, int[] UCArray) {
    return TauArgusJavaJNI.TauArgus_UnsafeVariableCodes(swigCPtr, this, VarIndex, CodeIndex, IsMissing, Freq, Code, Count, UCArray);
  }

  public boolean GetVarCode(int VarIndex, int CodeIndex, int[] CodeType, java.lang.String[] CodeString, int[] IsMissing, int[] Level) {
    return TauArgusJavaJNI.TauArgus_GetVarCode(swigCPtr, this, VarIndex, CodeIndex, CodeType, CodeString, IsMissing, Level);
  }

  public int GetVarHierarchyDepth(int VarIndex, boolean Recoded) {
    return TauArgusJavaJNI.TauArgus_GetVarHierarchyDepth(swigCPtr, this, VarIndex, Recoded);
  }

  public int SetHierarchicalCodelist(int VarIndex, String FileName, String LevelString) {
    return TauArgusJavaJNI.TauArgus_SetHierarchicalCodelist(swigCPtr, this, VarIndex, FileName, LevelString);
  }

  public boolean SetSecondaryHITAS(int TableIndex, int[] nSetSecondary) {
    return TauArgusJavaJNI.TauArgus_SetSecondaryHITAS(swigCPtr, this, TableIndex, nSetSecondary);
  }

  public boolean PrepareHITAS(int TableIndex, String NameParameterFile, String NameFilesFile, String TauTemp) {
    return TauArgusJavaJNI.TauArgus_PrepareHITAS(swigCPtr, this, TableIndex, NameParameterFile, NameFilesFile, TauTemp);
  }

  public boolean SetTableSafety(int Index, boolean DominanceRule, int[] DominanceNumber, int[] DominancePerc, boolean PQRule, int[] PriorPosteriorP, int[] PriorPosteriorQ, int[] PriorPosteriorN, int[] SafeMinRecAndHoldings, int[] PeepPerc, int[] PeepSafetyRange, int[] PeepMinFreqCellAndHolding, boolean ApplyPeep, boolean ApplyWeight, boolean ApplyWeightOnSafetyRule, boolean ApplyHolding, boolean ApplyZeroRule, boolean EmptyCellAsNonStructural, int NSEmptySafetyRange, double ZeroSafetyRange, int ManualSafetyPerc, int[] CellAndHoldingFreqSafetyPerc) {
    return TauArgusJavaJNI.TauArgus_SetTableSafety(swigCPtr, this, Index, DominanceRule, DominanceNumber, DominancePerc, PQRule, PriorPosteriorP, PriorPosteriorQ, PriorPosteriorN, SafeMinRecAndHoldings, PeepPerc, PeepSafetyRange, PeepMinFreqCellAndHolding, ApplyPeep, ApplyWeight, ApplyWeightOnSafetyRule, ApplyHolding, ApplyZeroRule, EmptyCellAsNonStructural, NSEmptySafetyRange, ZeroSafetyRange, ManualSafetyPerc, CellAndHoldingFreqSafetyPerc);
  }

  public boolean GetTableCellValue(int TableIndex, int CellIndex, double[] CellResponse) {
    return TauArgusJavaJNI.TauArgus_GetTableCellValue(swigCPtr, this, TableIndex, CellIndex, CellResponse);
  }

  public boolean GetTableCell(int TableIndex, int[] DimIndex, double[] CellResponse, double[] CellRoundedResp, double[] CellCTAResp, double[] CellCKMResp, double[] CellShadow, double[] CellCost, double[] CellKey, int[] CellFreq, int[] CellStatus, double[] CellMaxScore, double[] CellMaxScoreWeight, int[] HoldingFreq, double[] HoldingMaxScore, int[] HoldingNrPerMaxScore, double[] PeepCell, double[] PeepHolding, int[] PeepSortCell, int[] PeepSortHolding, double[] Lower, double[] Upper, double[] RealizedLower, double[] RealizedUpper) {
    return TauArgusJavaJNI.TauArgus_GetTableCell(swigCPtr, this, TableIndex, DimIndex, CellResponse, CellRoundedResp, CellCTAResp, CellCKMResp, CellShadow, CellCost, CellKey, CellFreq, CellStatus, CellMaxScore, CellMaxScoreWeight, HoldingFreq, HoldingMaxScore, HoldingNrPerMaxScore, PeepCell, PeepHolding, PeepSortCell, PeepSortHolding, Lower, Upper, RealizedLower, RealizedUpper);
  }

  public boolean SetTable(int Index, int nDim, int[] ExplanatoryVarList, boolean IsFrequencyTable, int ResponseVar, int ShadowVar, int CostVar, int CellKeyVar, String CKMType, int CKMTopK, double Lambda, double MaxScaledCost, int PeepVarnr, boolean SetMissingAsSafe) {
    return TauArgusJavaJNI.TauArgus_SetTable(swigCPtr, this, Index, nDim, ExplanatoryVarList, IsFrequencyTable, ResponseVar, ShadowVar, CostVar, CellKeyVar, CKMType, CKMTopK, Lambda, MaxScaledCost, PeepVarnr, SetMissingAsSafe);
  }

  public boolean SetVariable(int VarIndex, int bPos, int nPos, int nDec, int nMissing, String Missing1, String Missing2, String TotalCode, boolean IsPeeper, String PeeperCode1, String PeeperCode2, boolean IsCategorical, boolean IsNumeric, boolean IsWeight, boolean IsHierarchical, boolean IsHolding, boolean IsRecordKey) {
    return TauArgusJavaJNI.TauArgus_SetVariable(swigCPtr, this, VarIndex, bPos, nPos, nDec, nMissing, Missing1, Missing2, TotalCode, IsPeeper, PeeperCode1, PeeperCode2, IsCategorical, IsNumeric, IsWeight, IsHierarchical, IsHolding, IsRecordKey);
  }

  public boolean DoActiveRecode(int VarIndex) {
    return TauArgusJavaJNI.TauArgus_DoActiveRecode(swigCPtr, this, VarIndex);
  }

  public boolean GetVarNumberOfCodes(int VarIndex, int[] NumberOfCodes, int[] NumberOfActiveCodes) {
    return TauArgusJavaJNI.TauArgus_GetVarNumberOfCodes(swigCPtr, this, VarIndex, NumberOfCodes, NumberOfActiveCodes);
  }

  public boolean SetVarCodeActive(int VarIndex, int CodeIndex, boolean Active) {
    return TauArgusJavaJNI.TauArgus_SetVarCodeActive(swigCPtr, this, VarIndex, CodeIndex, Active);
  }

  public boolean GetStatusAndCostPerDim(int TableIndex, int[] Status, double[] Cost) {
    return TauArgusJavaJNI.TauArgus_GetStatusAndCostPerDim(swigCPtr, this, TableIndex, Status, Cost);
  }

  public boolean SetTableCellStatus(int TableIndex, int[] DimIndex, int CelStatus) {
    return TauArgusJavaJNI.TauArgus_SetTableCellStatus(swigCPtr, this, TableIndex, DimIndex, CelStatus);
  }

  public boolean UndoRecode(int VarIndex) {
    return TauArgusJavaJNI.TauArgus_UndoRecode(swigCPtr, this, VarIndex);
  }

  public int GetMaxnUc() {
    return TauArgusJavaJNI.TauArgus_GetMaxnUc(swigCPtr, this);
  }

  public boolean ExploreFile(String FileName, int[] ErrorCode, int[] LineNumber, int[] ErrorVarIndex) {
    return TauArgusJavaJNI.TauArgus_ExploreFile(swigCPtr, this, FileName, ErrorCode, LineNumber, ErrorVarIndex);
  }

  public boolean UnsafeVariable(int VarIndex, int[] Count, int[] UCArray) {
    return TauArgusJavaJNI.TauArgus_UnsafeVariable(swigCPtr, this, VarIndex, Count, UCArray);
  }

  public boolean GetTableRow(int TableIndex, int[] DimIndex, double[] Cell, int[] Status, int CountType) {
    return TauArgusJavaJNI.TauArgus_GetTableRow(swigCPtr, this, TableIndex, DimIndex, Cell, Status, CountType);
  }

  public boolean SetHierarchicalDigits(int VarIndex, int nDigitPairs, int[] nDigits) {
    return TauArgusJavaJNI.TauArgus_SetHierarchicalDigits(swigCPtr, this, VarIndex, nDigitPairs, nDigits);
  }

  public void CleanAll() {
    TauArgusJavaJNI.TauArgus_CleanAll(swigCPtr, this);
  }

  public void ApplyRecode() {
    TauArgusJavaJNI.TauArgus_ApplyRecode(swigCPtr, this);
  }

  public boolean DoRecode(int VarIndex, String RecodeString, int nMissing, String eMissing1, String eMissing2, int[] ErrorType, int[] ErrorLine, int[] ErrorPos, String[] WarningString) {
    return TauArgusJavaJNI.TauArgus_DoRecode(swigCPtr, this, VarIndex, RecodeString, nMissing, eMissing1, eMissing2, ErrorType, ErrorLine, ErrorPos, WarningString);
  }

  public boolean ComputeTables(int[] ErrorCode, int[] TableIndex) {
    return TauArgusJavaJNI.TauArgus_ComputeTables(swigCPtr, this, ErrorCode, TableIndex);
  }

  public boolean SetNumberTab(int nTab) {
    return TauArgusJavaJNI.TauArgus_SetNumberTab(swigCPtr, this, nTab);
  }

  public boolean SetNumberVar(int nVar) {
    return TauArgusJavaJNI.TauArgus_SetNumberVar(swigCPtr, this, nVar);
  }

  public double GetMinimumCellValue(int TableIndex, double[] Maximum) {
    return TauArgusJavaJNI.TauArgus_GetMinimumCellValue(swigCPtr, this, TableIndex, Maximum);
  }

  public boolean SetProtectionLevelsForResponseTable(int TableIndex, int[] DimIndex, double LowerBound, double UpperBound) {
    return TauArgusJavaJNI.TauArgus_SetProtectionLevelsForResponseTable(swigCPtr, this, TableIndex, DimIndex, LowerBound, UpperBound);
  }

  public String GetErrorString(int ErrorNumber) {
    return TauArgusJavaJNI.TauArgus_GetErrorString(swigCPtr, this, ErrorNumber);
  }

  public int SetCellKeyValuesFreq(int TabNo, String PTableFile, int[] MinDiff, int[] MaxDiff) {
    return TauArgusJavaJNI.TauArgus_SetCellKeyValuesFreq(swigCPtr, this, TabNo, PTableFile, MinDiff, MaxDiff);
  }

  public int SetCellKeyValuesCont(int TabNo, String PTableFileCont, String PTableFileSep, String CMKType, int topK, boolean IncludeZeros, boolean Parity, boolean Separation, double m1sqr, String Scaling, double s0, double s1, double xstar, double q, double[] epsilon) {
    return TauArgusJavaJNI.TauArgus_SetCellKeyValuesCont(swigCPtr, this, TabNo, PTableFileCont, PTableFileSep, CMKType, topK, IncludeZeros, Parity, Separation, m1sqr, Scaling, s0, s1, xstar, q, epsilon);
  }

}
