package vn.evolus.simpleapi.sample.controller;

import java.util.Date;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TestController {
    
    @RequestMapping(value="/test", method=RequestMethod.GET)
    @ResponseBody
    public String test() {
        return "Hehe: " + new Date();
    }
}
