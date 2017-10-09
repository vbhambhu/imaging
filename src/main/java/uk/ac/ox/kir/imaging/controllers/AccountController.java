package uk.ac.ox.kir.imaging.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ox.kir.imaging.models.User;
import uk.ac.ox.kir.imaging.repositories.UserRepository;

import javax.validation.Valid;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class AccountController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    public JavaMailSender emailSender;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(){
        return "account/login";
    }


    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String registerForm(User user){
        return "account/register";
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String registerCheck(@Valid User user, BindingResult bindingResult){

        String tempPassword = randomAlphaNumeric(10);

        if(!isValidUsername(user.getUsername())){
            bindingResult.rejectValue("username", "username", "Invalid username.");
        } else if(!usernameExists(user.getUsername())){
            bindingResult.rejectValue("username", "username", "Username already exists, please login.");
        } else if(!emailExists(user.getEmail())){
            bindingResult.rejectValue("email", "email", "Email address already exists, please login.");
        }

        if (bindingResult.hasErrors()) {
            return "account/register";
        }

        //Create user
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        user.setTempPassword(passwordEncoder.encode(tempPassword));
        user.setStatus(-1);
        user.setRole("USER");
        userRepository.save(user);

        String msg = "Thank you for registering KIR competetion. Your password is "+ tempPassword;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Verify your account.");
        message.setText(msg);
        emailSender.send(message);

        return "redirect:/register/verifiy";
    }

    @RequestMapping(value = "/register/verifiy", method = RequestMethod.GET)
    public String verifyAccountMessage(Model model){
        model.addAttribute("message", "An email sent to you. Please verify your email.");
        return "message";
    }

    @RequestMapping(value = "/updatepassword", method = RequestMethod.GET)
    public String updatePassword(Model model){

        return "account/updatepassword";
    }


    @RequestMapping(value = "/updatepassword", method = RequestMethod.POST)
    public String updatePasswords(Model model, Principal principal,
                                  @RequestParam(name = "password") String password,
                                  @RequestParam(name = "confirmpassword") String confirmpassword){


        if(password.isEmpty()){
            model.addAttribute("errMsg", "Password field is required.");
            return "account/updatepassword";
        } else if(password.length() < 5){
            model.addAttribute("errMsg", "Password field must be minimum 5 characters in length.");
            return "account/updatepassword";
        } else if(!password.equals(confirmpassword)){
            model.addAttribute("errMsg", "Password and Confirm password field did not match.");
            return "account/updatepassword";
        }

        //update password

        System.out.println(principal.getName());

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        User user = userRepository.findByUsername(principal.getName());
        user.setPassword(passwordEncoder.encode(password));
        user.setTempPassword(null);
        user.setStatus(1);
        userRepository.save(user);

        return "redirect:/dashboard";

    }


    @RequestMapping(value = "/forgot-password", method = RequestMethod.GET)
    public String forgotPassForm(){
        return "account/forgot";
    }

    @RequestMapping(value = "/forgot-password", method = RequestMethod.POST)
    public String forgotPass(Model model,
                             @RequestParam(name = "email") String email){

        System.out.println(email);

        if(email.isEmpty()){
            model.addAttribute("errMsg", "Email field is required.");
            return "account/forgot";
        } else if(!isValidEmailAddress(email) || !email.contains("ox.ac.uk")){
            model.addAttribute("errMsg", "Email field must be valid email address.");
            return "account/forgot";
        }


        User user = userRepository.findByEmail(email);

        if(user != null){
            //Update temp password, make passnull, status = -1
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String tempPassword = randomAlphaNumeric(10);
            user.setPassword(null);
            user.setTempPassword(passwordEncoder.encode(tempPassword));
            user.setStatus(-1);
            userRepository.save(user);
            //send reset email

            String msg = "Your new password is "+ tempPassword;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Reset password.");
            message.setText(msg);
            emailSender.send(message);
        }


        model.addAttribute("message", "Reset email sent.Please check your email and follow the instructions.");
        return "message";


    }

    public boolean isValidEmailAddress(String email) {

        String emailPattern = "^[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
        Pattern p = Pattern.compile(emailPattern);
        Matcher m = p.matcher(email);
        return m.matches();
    }





    private boolean isValidUsername(String username){

        String USERNAME_PATTERN = "^[a-z0-9_-]{3,15}$";
        Pattern pattern = Pattern.compile(USERNAME_PATTERN);
        Matcher matcher = pattern.matcher(username);
        return matcher.matches();
    }


    private String randomAlphaNumeric(int count) {

        String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    private boolean usernameExists(String username){
        User user = userRepository.findByUsername(username);
        return (user == null) ? true : false;
    }

    private boolean emailExists(String email){
        User user = userRepository.findByEmail(email);
        return (user == null) ? true : false;
    }
}
