package kr.flowdesk.domain;

/**
 * 인바디 체성분 측정 1건
 */
public class InbodyRecord {

  private String id;
  private String measuredAt;
  private Double heightCm;
  private Integer age;
  private String gender;
  private Double weightKg;
  private Double skeletalMuscleKg;
  private Double bodyFatMassKg;
  private Double totalBodyWaterKg;
  private Double proteinKg;
  private Double mineralKg;
  private Double fatFreeMassKg;
  private Double bmi;
  private Double percentBodyFat;
  private Double waistHipRatio;
  private Integer bmrKcal;
  private Integer visceralFatLevel;
  private Integer fitnessScore;
  private Double muscleControlKg;
  private Double fatControlKg;
  private String note;
  private String createUser;
  private String createDt;
  private String modifyDt;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getMeasuredAt() { return measuredAt; }
  public void setMeasuredAt(String measuredAt) { this.measuredAt = measuredAt; }

  public Double getHeightCm() { return heightCm; }
  public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }

  public Integer getAge() { return age; }
  public void setAge(Integer age) { this.age = age; }

  public String getGender() { return gender; }
  public void setGender(String gender) { this.gender = gender; }

  public Double getWeightKg() { return weightKg; }
  public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

  public Double getSkeletalMuscleKg() { return skeletalMuscleKg; }
  public void setSkeletalMuscleKg(Double skeletalMuscleKg) { this.skeletalMuscleKg = skeletalMuscleKg; }

  public Double getBodyFatMassKg() { return bodyFatMassKg; }
  public void setBodyFatMassKg(Double bodyFatMassKg) { this.bodyFatMassKg = bodyFatMassKg; }

  public Double getTotalBodyWaterKg() { return totalBodyWaterKg; }
  public void setTotalBodyWaterKg(Double totalBodyWaterKg) { this.totalBodyWaterKg = totalBodyWaterKg; }

  public Double getProteinKg() { return proteinKg; }
  public void setProteinKg(Double proteinKg) { this.proteinKg = proteinKg; }

  public Double getMineralKg() { return mineralKg; }
  public void setMineralKg(Double mineralKg) { this.mineralKg = mineralKg; }

  public Double getFatFreeMassKg() { return fatFreeMassKg; }
  public void setFatFreeMassKg(Double fatFreeMassKg) { this.fatFreeMassKg = fatFreeMassKg; }

  public Double getBmi() { return bmi; }
  public void setBmi(Double bmi) { this.bmi = bmi; }

  public Double getPercentBodyFat() { return percentBodyFat; }
  public void setPercentBodyFat(Double percentBodyFat) { this.percentBodyFat = percentBodyFat; }

  public Double getWaistHipRatio() { return waistHipRatio; }
  public void setWaistHipRatio(Double waistHipRatio) { this.waistHipRatio = waistHipRatio; }

  public Integer getBmrKcal() { return bmrKcal; }
  public void setBmrKcal(Integer bmrKcal) { this.bmrKcal = bmrKcal; }

  public Integer getVisceralFatLevel() { return visceralFatLevel; }
  public void setVisceralFatLevel(Integer visceralFatLevel) { this.visceralFatLevel = visceralFatLevel; }

  public Integer getFitnessScore() { return fitnessScore; }
  public void setFitnessScore(Integer fitnessScore) { this.fitnessScore = fitnessScore; }

  public Double getMuscleControlKg() { return muscleControlKg; }
  public void setMuscleControlKg(Double muscleControlKg) { this.muscleControlKg = muscleControlKg; }

  public Double getFatControlKg() { return fatControlKg; }
  public void setFatControlKg(Double fatControlKg) { this.fatControlKg = fatControlKg; }

  public String getNote() { return note; }
  public void setNote(String note) { this.note = note; }

  public String getCreateUser() { return createUser; }
  public void setCreateUser(String createUser) { this.createUser = createUser; }

  public String getCreateDt() { return createDt; }
  public void setCreateDt(String createDt) { this.createDt = createDt; }

  public String getModifyDt() { return modifyDt; }
  public void setModifyDt(String modifyDt) { this.modifyDt = modifyDt; }
}
