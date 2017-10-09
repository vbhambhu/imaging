package uk.ac.ox.kir.imaging.controllers;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ox.kir.imaging.repositories.CategoryRepository;
import uk.ac.ox.kir.imaging.repositories.EntryRepository;

import java.security.Principal;

@Controller
public class HomeController {

    @Autowired
    EntryRepository entryRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Value("${upload.path}")
    private String uploadPath;


    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String showHome(Model model) {


        model.addAttribute("entries", entryRepository.findAllByStatus(1));
        model.addAttribute("categories", categoryRepository.findAll());
        return "home";
    }

    @RequestMapping(value = "/dashboard", method = RequestMethod.GET)
    public String dashboard(Model model, Principal principal) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        System.out.println(authentication.getAuthorities().toString());

        model.addAttribute("uploadPath", uploadPath);
        model.addAttribute("entries", entryRepository.findAllByUsername(principal.getName()));
        return "dashboard";
    }




}
