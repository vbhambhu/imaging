package uk.ac.ox.kir.imaging.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ox.kir.imaging.models.Entry;
import uk.ac.ox.kir.imaging.repositories.CategoryRepository;
import uk.ac.ox.kir.imaging.repositories.EntryRepository;

import javax.jws.WebParam;
import javax.validation.Valid;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
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




    @RequestMapping(value = "/entry", method = RequestMethod.GET)
    public String showEntry(@RequestParam(name = "id", required = true) int entryId, Model model) {

        Entry entry = entryRepository.findOne(entryId);

        model.addAttribute("entry", entry);
        return "entry/show";
    }


    @RequestMapping(value = "/add/entry", method = RequestMethod.GET)
    public String entryForm(Entry entry, Model model) {

        model.addAttribute("categories", categoryRepository.findAll());
        return "entry/addmedia";
    }


    @RequestMapping(value = "/add/entry", method = RequestMethod.POST)
    public String entryCreate(Model model,
                              @RequestParam(name = "category_id") int categoryId,
                              @RequestParam(name = "file", required = true) MultipartFile fileData,
                              Principal principal) {

        model.addAttribute("categories", categoryRepository.findAll());






        //check if user uploaded file in same category

        Entry entry = entryRepository.findByCategoryIdAndUsername(categoryId, principal.getName());

        if(entry != null){
            model.addAttribute("errMsg", "You can upload one entry in each category only.");
            return "entry/addmedia";
        }

        List<String> contentTypes = Arrays.asList("image/png", "image/jpeg",
                "image/gif", "video/mp4","video/quicktime");


        String filename = fileData.getOriginalFilename();

        if(filename.isEmpty()){
            model.addAttribute("errMsg", "File field is required.");
            return "entry/addmedia";
        }

        String extension = filename.substring(filename.lastIndexOf("."), filename.length());

        String filepath = Paths.get(uploadPath, filename).toString();
        String fileContentType = fileData.getContentType();

        System.out.println(fileData.getContentType());



        if(!contentTypes.contains(fileContentType)){
            model.addAttribute("errMsg", "File you are trying to upload is not allowed.");
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
    public String editMedia(Principal principal,
                            @RequestParam(name = "id", required = true) int entryId,
                            @RequestParam(name = "file", required = true) MultipartFile fileData,
                            Model model
                            ){

        Entry entry = entryRepository.findByIdAndUsername(entryId, principal.getName());

        if(entry == null){ // do nothing
            return "redirect:/dashboard";
        }

        String filename = fileData.getOriginalFilename();
        String fileContentType = fileData.getContentType();
        List<String> contentTypes = Arrays.asList("image/png", "image/jpeg",
                "image/gif", "video/mp4","video/quicktime");

        if(filename.isEmpty()){
            model.addAttribute("errMsg", "File field is required.");
            return "entry/edit_media";
        } else if(!contentTypes.contains(fileContentType)){
            model.addAttribute("errMsg", "File you are trying to upload is not allowed.");
            return "entry/edit_media";
        }

        String mediaPath = uplaodMedia(fileData);

        String mediaType = (fileContentType.contains("image")) ? "image" : "video";


        entry.setMediaPath(mediaPath);
        entry.setType(mediaType);
        entryRepository.save(entry);

        return "redirect:/dashboard";
    }


    @RequestMapping(value = "entry/delete", method = RequestMethod.GET)
    public String deleteEntryForm(@RequestParam(name = "id", required = true) int entryId){
        return "entry/delete_entry";
    }

    @RequestMapping(value = "entry/delete", method = RequestMethod.POST)
    public String deleteEntry(@RequestParam(name = "id", required = true) int entryId,
                              Principal principal){

        Entry entry = entryRepository.findByIdAndUsername(entryId, principal.getName());

        if(entry != null){
            entryRepository.delete(entry);
        }

        return "redirect:/dashboard";
    }


    private String uplaodMedia(MultipartFile fileData){

        String filename = fileData.getOriginalFilename();
        String extension = filename.substring(filename.lastIndexOf("."), filename.length());
        String filepath = Paths.get(uploadPath, filename).toString();
        String fileContentType = fileData.getContentType();

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

        return newFileName;


    }


    @RequestMapping(value = "image/{name}")
    @ResponseBody
    public String getImage(@PathVariable(value = "name") String name) throws IOException {


        System.out.println(name);

        //File serverFile = new File(uploadPath+"/"+imageName);

        //return Files.readAllBytes(serverFile.toPath());
        return "dd";
    }



}
