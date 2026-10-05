package com.ppsu.xmldemo.controller;

import com.ppsu.xmldemo.model.Student;
import com.ppsu.xmldemo.service.DbCheck;
import com.ppsu.xmldemo.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class StudentController {

    @Autowired private StudentService studentService; // bean from the root context
    @Autowired private DbCheck dbCheck;

    @GetMapping("/students")
    public String list(Model model) {
        model.addAttribute("students", studentService.list());
        return "students"; // -> /WEB-INF/views/students.jsp
    }

    @PostMapping("/students")
    public String add(@RequestParam String name, @RequestParam String email, @RequestParam String course) {
        Student s = new Student();
        s.setName(name);
        s.setEmail(email);
        s.setCourse(course);
        studentService.add(s);
        return "redirect:/students";
    }

    @PostMapping("/students/{id}/delete")
    public String delete(@PathVariable Long id) {
        studentService.remove(id);
        return "redirect:/students";
    }

    @GetMapping(value = "/db-check", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String dbCheck() { return dbCheck.check(); }
}
