package ml.enzodevelopment.enzobot.utils;

import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.events.ReadyEvent;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

public class StatsUpdater {

    public void StartupdateTimer(ReadyEvent event) {
        Logger logger = LoggerFactory.getLogger(this.getClass().getName());

        int MINUTES = 5;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                event.getJDA().getPresence().setGame(Game.watching(event.getJDA().getGuildCache().size() + " servers! | g-bot.tk"));
                String token = Config.API_Key;
                String botId = "391558265265192961";

                int serverCount = (int) event.getJDA().getGuildCache().size();

                Config.DB.run(() -> {
                   try {
                       Connection conn = Config.DB.getConnManager().getConnection();
                       PreparedStatement stmt = conn.prepareStatement("UPDATE `API` SET `server_count` = ? WHERE `API`.`ID` = 1;");
                       stmt.setInt(1, serverCount);
                       stmt.executeUpdate();
                   } catch (SQLException ex) {
                       logger.error("Database Error", ex);
                   }
                });
                OkHttpClient client = new OkHttpClient();
                FormBody body = new FormBody.Builder().add("server_count", String.valueOf(serverCount)).build();
                Request request = new Request.Builder().url("https://discordbots.org/api/bots/" + botId + "/stats").post(body).addHeader("Authorization", token).addHeader("Content-Type", "application/json").build();
                try {
                    client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000 * 60 * MINUTES);
    }
}