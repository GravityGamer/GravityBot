package gravity.gbot.commands.basic;

import gravity.gbot.Command;
import gravity.gbot.utils.Config;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class RollCommand implements Command {

    @Override
    public void execute(String[] args, GuildMessageReceivedEvent event) {
        if (args.length == 2) {
            int max = Integer.parseInt(args[1]);

            int outcome;

            EmbedBuilder rolling = new EmbedBuilder();
            rolling.setColor(Config.GBot_Blue);
            rolling.setTitle("Roll");
            rolling.setDescription("Rolling random number between 0 and " + max);
            event.getChannel().sendMessage(rolling.build()).queue();
            event.getChannel().sendTyping().queue();

            outcome = (int) (Math.random() * max + 1);

            EmbedBuilder result = new EmbedBuilder();
            result.setColor(Config.GBot_Blue);
            result.setTitle("Success");
            result.setDescription("Your number is " + outcome + "!");
            event.getChannel().sendMessage(result.build()).queue();

        } else {
            EmbedBuilder error = new EmbedBuilder();
            error.setTitle("Error");
            error.setDescription("Invalid command usage!");
            error.setColor(Config.GBot_Blue);
            event.getChannel().sendMessage(error.build()).queue();
        }
    }

    @Override
    public String cmdUsage() {
        return "Roll (Maximum Number)";
    }

    @Override
    public String cmdDesc() {
        return "Rolls a random number between 0 and the supplied number.";
    }

    @Override
    public String getAlias() {
        return "roll";
    }

    @Override
    public String cmdType() {
        return "public";
    }
}
