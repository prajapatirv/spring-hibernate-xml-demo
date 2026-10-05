package com.ppsu.xmldemo.service;

import com.ppsu.xmldemo.dao.StudentDao;
import com.ppsu.xmldemo.model.Student;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class StudentService {
    private StudentDao studentDao;

    public void setStudentDao(StudentDao studentDao) { this.studentDao = studentDao; }

    // @Transactional opens the Session + transaction before, commits after
    @Transactional(readOnly = true)
    public List<Student> list() { return studentDao.findAll(); }

    @Transactional
    public void add(Student s) { studentDao.save(s); }

    @Transactional
    public void remove(Long id) { studentDao.deleteById(id); }
}
