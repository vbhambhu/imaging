package uk.ac.ox.kir.imaging.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ox.kir.imaging.models.Entry;
import uk.ac.ox.kir.imaging.repositories.CategoryRepository;
import uk.ac.ox.kir.imaging.repositories.EntryRepository;

import javax.validation.Valid;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Controller
public class EntryController {

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    EntryRepository entryRepository;

    @Value("${upload.path}")
    private String uploadPath;


    @RequestMapping(value = "/add/entry", method = RequestMethod.GET)
    public String entryForm(Entry entry, Model model) {

        model.addAttribute("categories", categoryRepository.findAll());
        return "entry/addmedia";
    }


    @RequestMapping(value = "/add/entry", method = RequestMethod.POST)
    public String entryCreate(Model model,
                              @RequestParam(name = "category_id") int categoryId,
                              @RequestParam(name = "file") MultipartFile fileData,
                              Principal principal) {

        model.addAttribute("categories", categoryRepository.findAll());

        //check if user uploaded file in same category

        Entry entry = entryRepository.findByCategoryIdAndUsername(categoryId, principal.getName());

        if(entry != null){
            model.addAttribute("errMsg", "You can upload one entry in each category only.");
            return "entry/addmedia";
        }

        List<String> contentTypes = Arrays.asList("image/png", "image/jpeg",
                "image/gif", "video/mp4");


        String filename = fileData.getOriginalFilename();
        String extension = filename.substring(filename.lastIndexOf("."), filename.length());

        String filepath = Paths.get(uploadPath, filename).toString();
        String fileContentType = fileData.getContentType();

        System.out.println(fileData.getContentType());

        if(!contentTypes.contains(fileContentType)){
            model.addAttribute("errMsg", "You cant upload this.");
            return "entry/addmedia";
        }


        String type = "";

        if(fileContentType.contains("image")){
            type = "image";
        } else{
            type = "video";
        }

        //add entry
        System.out.println(fileData.getContentType());
        System.out.println(filepath);

        File file = new File(filepath);
        try {
            BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(file));
            stream.write(fileData.getBytes());
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String newFileName = UUID.randomUUID().toString()+extension;
        String newfilepath = Paths.get(uploadPath, newFileName).toString();
        file.renameTo(new File(newfilepath));

        Entry ent = new Entry();
        ent.setTitle("Your title here");
        ent.setDescription("Add some description");
        ent.setCategoryId(categoryId);
        ent.setType(type);
        ent.setUsername(principal.getName());
        ent.setMediaPath(newFileName);
        ent.setVotes(0);
        ent.setUpdatedAt(new Date());
        entryRepository.save(ent);

        return "redirect:/entry/edit/details?id="+ent.getId();

    }


    @RequestMapping(value = "entry/edit/details", method = RequestMethod.GET)
    public String editDetailsForm(Model model,
                                  @RequestParam(name = "id", required = true) int entryId){

        Entry entry = entryRepository.findOne(entryId);

        model.addAttribute("entry", entry);

        return "entry/edit_details";

    }

    @RequestMapping(value = "entry/edit/details", method = RequestMethod.POST)
    public String editDetails(@Valid Entry entry, BindingResult bindingResult,
                              Principal principal){

        if(bindingResult.hasErrors()){
            return "entry/edit_details";
        }

        entry.setUsername(principal.getName());
        entry.setUpdatedAt(new Date());

        entryRepository.save(entry);

        return "redirect:/dashboard";
    }


    @RequestMapping(value = "entry/edit/media", method = RequestMethod.GET)
    public String editMediaForm(@RequestParam(name = "id", required = true) int entryId){
        return "entry/edit_media";
    }

    @RequestMapping(value = "entry/edit/media", method = RequestMethod.POST)
    public String editMedia(Principal principal){



        return "redirect:/dashboard";
    }



}
