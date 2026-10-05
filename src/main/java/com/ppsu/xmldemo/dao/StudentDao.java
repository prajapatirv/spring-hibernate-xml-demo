package com.ppsu.xmldemo.dao;

import com.ppsu.xmldemo.model.Student;
import org.hibernate.SessionFactory;

import java.util.List;

public class StudentDao {
    private SessionFactory sessionFactory;

    // Injected from applicationContext.xml
    public void setSessionFactory(SessionFactory sessionFactory) { this.sessionFactory = sessionFactory; }

    // Session = one unit of work. getCurrentSession() only works inside a transaction.
    public List<Student> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("from Student order by id", Student.class).list();
    }

    public void save(Student s) { sessionFactory.getCurrentSession().save(s); }

    public void deleteById(Long id) {
        var session = sessionFactory.getCurrentSession();
        Student s = session.get(Student.class, id);
        if (s != null) session.delete(s);
    }
}
