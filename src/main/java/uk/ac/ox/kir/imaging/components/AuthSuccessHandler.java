package uk.ac.ox.kir.imaging.components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import uk.ac.ox.kir.imaging.models.User;
import uk.ac.ox.kir.imaging.repositories.UserRepository;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthSuccessHandler implements AuthenticationSuccessHandler {


    @Autowired
    UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        if(isFirstTimeLogin(authentication.getName())) {
            response.sendRedirect("/updatepassword");
        } else {
            response.sendRedirect("/dashboard");
        }

    }

    private boolean isFirstTimeLogin(String username) {

        User user = userRepository.findByUsername(username);
        return (user.getStatus() == -1 && user.getPassword() == null) ? true : false;

    }

}
