DROP TABLE IF EXISTS infearlearn_students;

CREATE TABLE IF NOT EXISTS infearlearn_students (
    student_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    current_lecture VARCHAR(255),
    instructor VARCHAR(255),
    persuasion_method VARCHAR(255)
);