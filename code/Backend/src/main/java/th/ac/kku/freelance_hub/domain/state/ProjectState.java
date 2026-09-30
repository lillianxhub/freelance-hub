package th.ac.kku.freelance_hub.domain.state;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

public interface ProjectState {

    //สถานะของ Project
    ProjectStatus status();
    //ตรวจสอบว่าสถานะของ Project สามารถเปลี่ยนไปยังสถานะถัดไปได้หรือไม่
    boolean canTransitionTo(ProjectStatus nextStatus);

    //ตรวจสอบว่าสถานะของ Project สามารถติดตามเวลาได้หรือไม่
    boolean canTrackTime();

    //ตรวจสอบว่าสถานะของ Project สามารถแก้ไขงานได้หรือไม่
    boolean canEditTasks();
}
