package com.tdtu.phonecommerce.controller;


import com.tdtu.phonecommerce.dto.EmailRequest;
import com.tdtu.phonecommerce.dto.PasswordDTO;
import com.tdtu.phonecommerce.models.Product;
import com.tdtu.phonecommerce.models.Roles;
import com.tdtu.phonecommerce.models.User;
import com.tdtu.phonecommerce.service.EmailService;
import com.tdtu.phonecommerce.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.context.Context;

import java.util.ArrayList;
import java.util.List;

@Controller
@AllArgsConstructor
public class UserController {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    private final UserService userService;

    private final EmailService emailService;


    @GetMapping("/manager/employee")
    public String getEmployeePage(Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String currUserName = authentication.getName();


        List<User> userListManager = userService.findByRoles(Roles.ROLES_MANAGER);

        userListManager.removeIf(currUser -> currUser.getUserName().equals(currUserName));

        List<User> userListEmployee = userService.findByRoles(Roles.ROLES_EMPLOYEE);

        userListManager.addAll(userListEmployee);

        List<User> userListBlogger = userService.findByRoles(Roles.ROLES_BLOGGER);

        userListManager.addAll(userListBlogger);

        model.addAttribute("userList", userListManager);


        return "/manager_template/manager_employee";
    }


    @GetMapping("/manager/employee/add")
    public String getAddUserPage(Model model) {
        model.addAttribute("newUser", new User());

        List<Roles> roles = new ArrayList<>();

        roles.add(Roles.ROLES_EMPLOYEE);
        roles.add(Roles.ROLES_MANAGER);
        roles.add(Roles.ROLES_USER);

        model.addAttribute("roles", roles);


        return "manager_template/manager_add-employee";

    }

    @PostMapping("/manager/employee/add")
    public String registerForm(@ModelAttribute("newUser") User newUser, Model model) {

        String password = newUser.getPassword();

        newUser.setPassword(bCryptPasswordEncoder.encode(password));

        List<User> checkedUsers = userService.findByEmail(newUser.getEmail());

        if (!checkedUsers.isEmpty()) {
            model.addAttribute("errorRegister", "Đăng ký không thành công, email đã tồn tại");
            return "redirect:/manager/employee/add";
        }


        try {
            userService.saveUser(newUser);

            // gửi mail

            EmailRequest emailRequest = new EmailRequest();

            emailRequest.setTo(newUser.getEmail());

            emailRequest.setSubject("WELCOME TO OUR FAMILY");

            String body = "Your username is :" + newUser.getUserName() + "\n";
            body += "Your password is : " + password + "\n";

            emailRequest.setBody(body);

            Context context = new Context();

            context.setVariable("message", emailRequest.getBody());

            emailService.sendEmailWithHtmlTemplate(
                    emailRequest.getTo(),
                    emailRequest.getSubject(),
                    "email-template",
                    context);

            model.addAttribute("errorRegister", "Đăng ký thành công");
        } catch (Exception e) {
            model.addAttribute("errorRegister", "Đăng ký không thành công, email đã tồn tại");
        }

        return "redirect:/manager/employee/add";

    }

    @GetMapping("/manager/employee/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return "redirect:/manager/employee";
    }

    @GetMapping("/manager/employee/edit/{id}")
    public String editUserPage(@PathVariable Long id, Model model) {

        User editUser = userService.findById(id);

        List<Roles> roles = new ArrayList<>();

        roles.add(Roles.ROLES_EMPLOYEE);
        roles.add(Roles.ROLES_MANAGER);
        roles.add(Roles.ROLES_USER);


        model.addAttribute("editUser", editUser);
        model.addAttribute("rolesList", roles);

        return "manager_template/manager_edit-employee";
    }

    @PostMapping("/manager/employee/edit")
    public String editUserProcess(@ModelAttribute("editUser") User updatedUser) {

        List<User> users = userService.findByEmail(updatedUser.getEmail());

        if (!users.isEmpty()) {
            User checkedUser = users.get(0);

            if (!checkedUser.getId().equals(updatedUser.getId())) {
                return "redirect:/manager/employee";
            }

        }

        userService.saveUser(updatedUser);

        return "redirect:/manager/employee";
    }


    @PostMapping("/user/edit")
    public String editUserInfo(@ModelAttribute("editUser") User updateUser, RedirectAttributes redirectAttributes) {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();

        List<User> userList = userService.findByEmail(updateUser.getEmail());

        if (!userList.isEmpty()) {
            if (!userList.get(0).getUserName().equals(username)) {


                redirectAttributes.addFlashAttribute("errorMessage", "Email has existed");
                return "redirect:/user";
            }
        }

        User currentUser = userList.get(0);

        currentUser.setName(updateUser.getName());
        currentUser.setEmail(updateUser.getEmail());


        userService.saveUser(currentUser);

        redirectAttributes.addFlashAttribute("successMessage", "Update information successfully");
        return "redirect:/user";
    }

    @GetMapping("/user")
    public String getUserPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();

        User currentUser = userService.findByUsername(username);

        model.addAttribute("editUser", currentUser);

        return "user_details";

    }


    @GetMapping("/user/password")
    public String getChangePasswordPage(Model model) {

        PasswordDTO passwordDTO = new PasswordDTO();

        model.addAttribute("password", passwordDTO);

        return "change-password";
    }

    @PostMapping("/user/password/edit")
    public String changePassword(@ModelAttribute("password") PasswordDTO passwordDTO, RedirectAttributes redirectAttributes) {

        String currentPassword = passwordDTO.getCurrentPassword();
        String newPassword = passwordDTO.getNewPassword();
        String confPassword = passwordDTO.getConfPassword();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();

        User user = userService.findByUsername(username);

        String password = user.getPassword();

        boolean isMatch = bCryptPasswordEncoder.matches(currentPassword, password);


        if (!isMatch) {
            redirectAttributes.addFlashAttribute("errorMessage", "Wrong current password");
            return "redirect:/user/password";
        }

        if (!newPassword.equals(confPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Wrong confirm password");
            return "redirect:/user/password";
        }

        user.setPassword(bCryptPasswordEncoder.encode(confPassword));

        userService.saveUser(user);


        redirectAttributes.addFlashAttribute("successMessage", "Update password successfully");
        return "redirect:/user/password";

    }


}
