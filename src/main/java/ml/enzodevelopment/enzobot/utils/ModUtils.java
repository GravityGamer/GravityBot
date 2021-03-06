/*
 * Enzo Bot, a multipurpose discord bot
 *
 * Copyright (c) 2018 William "Enzo" Johnstone
 *
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published
 *  by the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package ml.enzodevelopment.enzobot.utils;

import ml.enzodevelopment.enzobot.config.Config;
import ml.enzodevelopment.enzobot.objects.ConsoleUser;
import ml.enzodevelopment.enzobot.objects.FakeUser;
import ml.enzodevelopment.enzobot.objects.punishment.PunishmentType;
import ml.enzodevelopment.enzobot.objects.warning.WarnObject;
import ml.enzodevelopment.enzobot.objects.warning.Warning;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ModUtils {

    private static Logger logger = LoggerFactory.getLogger(ModUtils.class);

    public static void modLog(User mod, User punishedUser, PunishmentType type, String reason, String time, Guild g) {
        String chan = GuildSettingsUtils.getGuild(g).getLogChannel();
        if (chan != null && !chan.isEmpty()) {
            TextChannel logChannel = g.getTextChannelById(chan);
            if (logChannel == null) {
                return;
            }
            String length = "";
            if (time != null && !time.isEmpty()) {
                String[] timeParts = time.split("(?<=\\D)+(?=\\d)+|(?<=\\d)+(?=\\D)+");
                String units = getTimeUnit(timeParts[1]);
                length = " for " + timeParts[0] + " " + units;
            }
            String punishment = getPunishmentString(type);

            String finalReason = reason.isEmpty() ? "" : " with reason `" + reason + "`";
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.WHITE);
            builder.setTitle("User " + punishment.substring(0, 1).toUpperCase() + punishment.substring(1));
            builder.setDescription("**" + mod.getName() + "#" + mod.getDiscriminator() + "** " + punishment + " **" + punishedUser.getName() + "#" + punishedUser.getDiscriminator() + "**" + length + finalReason);
            logChannel.sendMessage(builder.build()).queue();
        }
    }


    public static void modLog(User mod, User punishedUser, PunishmentType punishment, String reason, Guild g) {
        modLog(mod, punishedUser, punishment, reason, "", g);
    }

    public static void modLog(User mod, User unbannedUser, PunishmentType punishment, Guild g) {
        modLog(mod, unbannedUser, punishment, "", g);
    }

    public static void addBannedUserToDb(String modID, String userName, String userDiscriminator, String userId, String unbanDate, String guildId) {
        Config.DB.run(() -> {
            Connection conn = Config.DB.getConnManager().getConnection();
            try (PreparedStatement smt = conn.prepareStatement("INSERT INTO bans(modUserId, Username, discriminator, userId, ban_date, unban_date, guildId) " +
                    "VALUES(? , ? , ? , ? , NOW() , ?, ?)")) {
                smt.setString(1, modID);
                smt.setString(2, userName);
                smt.setString(3, userDiscriminator);
                smt.setString(4, userId);
                smt.setString(5, unbanDate);
                smt.setString(6, guildId);
                smt.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }

    public static void addMutedUserToDb(String modID, String userName, String userDiscriminator, String userId, String unmuteDate, String guildId) {
        Config.DB.run(() -> {
            Connection conn = Config.DB.getConnManager().getConnection();
            try (PreparedStatement smt = conn.prepareStatement("INSERT INTO mutes(modUserId, Username, discriminator, userId, mute_date, unmute_date, guildId) " +
                    "VALUES(? , ? , ? , ? , NOW() , ?, ?)")) {
                smt.setString(1, modID);
                smt.setString(2, userName);
                smt.setString(3, userDiscriminator);
                smt.setString(4, userId);
                smt.setString(5, unmuteDate);
                smt.setString(6, guildId);
                smt.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }

    public static void checkUnbans(JDA jda) {
        Config.DB.run(() -> {
            logger.debug("Checking for users to unban");
            int usersUnbanned = 0;
            Connection database = Config.DB.getConnManager().getConnection();

            try (Statement smt = database.createStatement()) {
                ResultSet res = smt.executeQuery("SELECT * FROM bans");

                while (res.next()) {
                    java.util.Date unbanDate = res.getTimestamp("unban_date");
                    java.util.Date currDate = new java.util.Date();

                    if (currDate.after(unbanDate)) {
                        usersUnbanned++;
                        String username = res.getString("Username");
                        logger.debug("Unbanning " + username);
                        try {
                            String guildId = res.getString("guildId");
                            String userID = res.getString("userId");
                            Guild guild = jda.getGuildById(guildId);
                            if (guild != null) {
                                guild.getController()
                                        .unban(userID).reason("Ban expired").queue();
                                modLog(new ConsoleUser(), new FakeUser(username, userID, res.getString("discriminator")), PunishmentType.UNBAN, "Ban Expired", guild);
                            }
                        } catch (NullPointerException ignored) {
                        }
                        database.createStatement().executeUpdate("DELETE FROM bans WHERE id=" + res.getInt("id") + "");
                    }
                }
                logger.debug("Checking done, unbanned " + usersUnbanned + " users.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void checkUnmutes(JDA jda) {
        Config.DB.run(() -> {
            logger.debug("Checking for users to unmute");
            int usersUnmuted = 0;
            Connection database = Config.DB.getConnManager().getConnection();

            try (Statement smt = database.createStatement()) {
                ResultSet res = smt.executeQuery("SELECT * FROM mutes");

                while (res.next()) {
                    java.util.Date unmuteDate = res.getTimestamp("unmute_date");
                    java.util.Date currDate = new java.util.Date();

                    if (currDate.after(unmuteDate)) {
                        usersUnmuted++;
                        String username = res.getString("Username");
                        logger.debug("Unmuting " + username);
                        try {
                            String guildId = res.getString("guildId");
                            String userID = res.getString("userId");
                            Guild guild = jda.getGuildById(guildId);
                            if (guild != null) {
                                Role muteRole = guild.getRoleById(GuildSettingsUtils.getGuild(guild).getMuteRoleId());
                                guild.getController()
                                        .removeSingleRoleFromMember(guild.getMember(jda.getUserById(userID)), muteRole).queue();
                                modLog(new ConsoleUser(), new FakeUser(username, userID, res.getString("discriminator")), PunishmentType.UNMUTE, "Mute Expired", guild);
                            }
                        } catch (NullPointerException ignored) {
                        }
                        database.createStatement().executeUpdate("DELETE FROM mutes WHERE id=" + res.getInt("id") + "");
                    }
                }
                logger.debug("Checking done, unmuted " + usersUnmuted + " users.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static int getWarningCountForUser(User u, Guild g) {
        if (u == null)
            throw new IllegalArgumentException("User to check can not be null");

        return getWarnsForUser(u.getId(), g.getId()).getWarnings().size();
    }

    public static void addWarningToDb(User moderator, User target, String reason, Guild guild) {

        Config.DB.run(() -> {
            Connection conn = Config.DB.getConnManager().getConnection();
            try (PreparedStatement smt = conn.prepareStatement("INSERT INTO warnings(modUserId, userId, reason, guildId, warnDate, expireDate) " +
                    "VALUES(? , ? , ? , ?  , CURDATE(), DATE_ADD(CURDATE(), INTERVAL 3 DAY) )")) {
                smt.setString(1, moderator.getId());
                smt.setString(2, target.getId());
                smt.setString(3, reason);
                smt.setString(4, guild.getId());
                smt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        });
    }

    private static String getPunishmentString(PunishmentType type) {
        String punishment = "";
        switch (type) {
            case BAN:
                punishment = "banned";
                break;
            case SOFTBAN:
                punishment = "soft banned";
                break;
            case KICK:
                punishment = "kicked";
                break;
            case MUTE:
                punishment = "muted";
                break;
            case WARN:
                punishment = "warned";
                break;
            case UNBAN:
                punishment = "unbanned";
                break;
            case UNMUTE:
                punishment = "unmuted";
                break;
            case TEMP_MUTE:
                punishment = "temp-muted";
                break;
            case TEMP_BAN:
                punishment = "temp-banned";
                break;
            default:
                punishment = "Unknown";
                break;
        }
        return punishment;
    }

    private static String getTimeUnit(String time) {
        String units = "";
        switch (time) {
            case "m":
                units = "minutes";
                break;
            case "h":
                units = "hour(s)";
                break;
            case "d":
                units = "day(s)";
                break;
            case "w":
                units = "week(s)";
                break;
            case "M":
                units = "month(s)";
                break;
            case "Y":
                units = "year(s)";
                break;
            default:
                units = "Unknown";
                break;
        }
        return units;
    }

    private static WarnObject getWarnsForUser(String userId, String guildId) {
        Connection conn = Config.DB.getConnManager().getConnection();

        String sql = "SELECT * FROM `warnings` WHERE userId=? AND guildId=? AND (CURDATE() <= DATE_ADD(expireDate, INTERVAL 3 DAY))";

        List<Warning> warnings = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, guildId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                warnings.add(new Warning(
                        rs.getInt("id"),
                        rs.getDate("warnDate"),
                        rs.getDate("expireDate"),
                        rs.getString("modUserId"),
                        rs.getString("reason"),
                        rs.getString("guildId")
                        ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new WarnObject(userId, warnings);
    }


    public static void sendSuccess(Message message) {
        if (message.getChannelType() == ChannelType.TEXT) {
            TextChannel channel = message.getTextChannel();
            if (channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_ADD_REACTION)) {
                message.addReaction("✅").queue(null, ignored -> {
                });
            }
        }
    }

}
