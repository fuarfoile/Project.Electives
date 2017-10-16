package com.boast.controller.command.impl;

import com.boast.controller.command.Command;
import com.boast.controller.command.Receiver;
import com.boast.model.util.InputChecker;
import com.boast.domain.builder.impl.UserBuilder;
import com.boast.model.dao.connection.impl.MySqlConnectionFactory;
import com.boast.model.dao.impl.MySqlDaoFactory;
import com.boast.domain.Position;
import com.boast.domain.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.Connection;
import java.util.Locale;
import java.util.ResourceBundle;

/** Комманда обновления данных аккаунта*/
public class CommandAccountUpdate implements Command {
    private static Logger logger = LogManager.getLogger(CommandAccountUpdate.class);

    @Override
    public String execute(HttpServletRequest request, HttpServletResponse response){
        HttpSession session = request.getSession(true);

        Receiver receiver = new Receiver(request, response);

        Locale locale = new Locale((String) session.getAttribute("language"));
        ResourceBundle resource = ResourceBundle.getBundle("localization/translation", locale);


        User oldUser = (User) session.getAttribute("user");
        User user = new UserBuilder()
                .setId(oldUser.getId())
                .setEmail(request.getParameter("login"))
                .setPassword(request.getParameter("password"))
                .setPosition(oldUser.getPosition())
                .setName(request.getParameter("name"))
                .setSurname(request.getParameter("surname"))
                .setPhoneNumber(request.getParameter("phoneNumber")).build();

        String repPassword = request.getParameter("rPassword");

        String errMsg = InputChecker.check(user) ? "" : resource.getString("error.input");
        boolean emailFound = false;

        if (user.getPassword().length() == 0 && repPassword.length() == 0) {
            user.setPassword(null);
        }else if (!user.getPassword().equals(repPassword)) {
            errMsg += resource.getString("account.update.message.error.password");
        }

        MySqlDaoFactory daoFactory = MySqlDaoFactory.getInstance();
        Connection connection = MySqlConnectionFactory.getInstance().getConnection();

        if (!user.getEmail().equalsIgnoreCase(oldUser.getEmail())) {
            emailFound = daoFactory.getUserDao(connection).isEmailInBase(user.getEmail());
        }

        if (emailFound) {
            if (errMsg.length() > 0) {
                errMsg += "\n";
            }

            errMsg += resource.getString("account.update.message.error.email");
        }

        if (errMsg.length() == 0) {
            session.setAttribute("user", user);
            session.setAttribute("AdminPosition", Position.ADMIN);

            if (request.getCookies().length > 0) {
                receiver.addCookie(user.getEmail(), user.getPassword());
            }

            if (daoFactory.getUserDao(connection).update(user)) {
                request.setAttribute("msg", resource.getString("account.update.message.success"));
            } else {
                logger.error("MySqlUserDao.update fail");
            }
        }

        request.setAttribute("position", user.getPosition());
        request.setAttribute ("login", user.getEmail());
        request.setAttribute ("name", user.getName());
        request.setAttribute ("surname", user.getSurname());
        request.setAttribute ("phoneNumber", user.getPhoneNumber());
        request.setAttribute("error_msg", errMsg);

        return receiver.rAccountUpdate();
    }
}
