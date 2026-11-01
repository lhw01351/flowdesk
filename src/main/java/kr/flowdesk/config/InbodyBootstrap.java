package kr.flowdesk.config;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import kr.flowdesk.domain.InbodyRecord;
import kr.flowdesk.service.content.InbodyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 인바디 컬럼 마이그레이션 + 시드 (사용자별 소유)
 * Order 3: 계정 시드(AuthDataBootstrap) 이후
 */
@Component
@Order(3)
public class InbodyBootstrap implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(InbodyBootstrap.class);
  private static final String DEFAULT_OWNER = "hwlee";

  private final DataSource dataSource;
  private final InbodyService inbodyService;

  public InbodyBootstrap(DataSource dataSource, InbodyService inbodyService) {
    this.dataSource = dataSource;
    this.inbodyService = inbodyService;
  }

  @Override
  public void run(ApplicationArguments args) {
    ensureOwnerColumns();
    int filled = inbodyService.backfillOwner(DEFAULT_OWNER);
    if (filled > 0) {
      log.info("인바디 create_user 보정 {}건 → {}", filled, DEFAULT_OWNER);
    }
    if (inbodyService.countByOwner(DEFAULT_OWNER) > 0 || inbodyService.count() > 0) {
      return;
    }
    log.info("인바디 시드 데이터 입력 (소유자={})", DEFAULT_OWNER);
    seedAll();
  }

  private void ensureOwnerColumns() {
    alter("ALTER TABLE inbody_records ADD COLUMN create_user VARCHAR(100)");
    alter("ALTER TABLE inbody_records ADD COLUMN create_dt VARCHAR(32)");
    alter("ALTER TABLE inbody_records ADD COLUMN modify_dt VARCHAR(32)");
  }

  private void alter(String sql) {
    try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
      st.execute(sql);
      log.info("스키마: {}", sql);
    } catch (Exception ignored) {
      // 이미 있으면 무시
    }
  }

  private void seedAll() {
    inbodyService.create(rec("ib-20260813", "2026-08-13", 35,
        92.5, 38.6, 25.0, 49.3, 13.5, 4.74, 67.5,
        29.2, 27.0, 0.93, 1828, 10, 75, 0.0, -13.1,
        "S Fitness 측정"), DEFAULT_OWNER);
    inbodyService.create(rec("ib-20260831", "2026-08-31", 35,
        90.6, 39.1, 22.4, 49.8, 13.6, 4.75, 68.2,
        28.6, 24.7, 0.90, 1843, 9, 79, 0.0, -10.4,
        "S Fitness 측정"), DEFAULT_OWNER);
    inbodyService.create(rec("ib-20260919", "2026-09-19", 36,
        89.1, 38.4, 21.8, 49.2, 13.3, 4.76, 67.3,
        28.1, 24.5, 0.91, 1823, 9, 78, 0.0, -9.9,
        "S Fitness 측정"), DEFAULT_OWNER);
    inbodyService.create(rec("ib-20261005", "2026-10-05", 36,
        87.6, 38.5, 20.2, 49.3, 13.4, 4.72, 67.4,
        27.6, 23.0, 0.90, 1826, 8, 80, 0.0, -8.3,
        "S Fitness 측정"), DEFAULT_OWNER);
    inbodyService.create(rec("ib-20261016", "2026-10-16", 35,
        87.1, 39.0, 18.8, 50.0, 13.5, 4.78, 68.3,
        27.5, 21.5, 0.87, 1846, 7, 82, 0.0, -6.7,
        "S Fitness 측정"), DEFAULT_OWNER);
    inbodyService.create(rec("ib-20261025", "2026-10-25", 36,
        85.5, 39.0, 18.8, 50.0, 13.5, 4.78, 68.3,
        27.0, 21.5, 0.87, 1846, 7, 82, 0.0, -6.7,
        "체중만 갱신 (10.16 대비 -1.6kg), 기타 수치는 10.16과 동일"), DEFAULT_OWNER);
  }

  private InbodyRecord rec(
      String id, String date, int age,
      double weight, double smm, double fat, double tbw, double protein, double mineral, double ffm,
      double bmi, double pbf, double whr, int bmr, int visceral, int score,
      double muscleCtrl, double fatCtrl, String note) {
    InbodyRecord r = new InbodyRecord();
    r.setId(id);
    r.setMeasuredAt(date);
    r.setHeightCm(178.0);
    r.setAge(age);
    r.setGender("남");
    r.setWeightKg(weight);
    r.setSkeletalMuscleKg(smm);
    r.setBodyFatMassKg(fat);
    r.setTotalBodyWaterKg(tbw);
    r.setProteinKg(protein);
    r.setMineralKg(mineral);
    r.setFatFreeMassKg(ffm);
    r.setBmi(bmi);
    r.setPercentBodyFat(pbf);
    r.setWaistHipRatio(whr);
    r.setBmrKcal(bmr);
    r.setVisceralFatLevel(visceral);
    r.setFitnessScore(score);
    r.setMuscleControlKg(muscleCtrl);
    r.setFatControlKg(fatCtrl);
    r.setNote(note);
    r.setCreateUser(DEFAULT_OWNER);
    return r;
  }
}
