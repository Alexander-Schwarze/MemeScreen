# MemeScreen
A program for Brett to black out parts of a screen after certain times

# Setup
When program is new, create a folder "data" on highest level (same as src), which contains following files with their content:
* bot.token
  * twitch-bot token is the only content in this file. Example of whole file content: abcE1134dshn42458325sfjsa
  * The bot token can be found on several applications working with twitch  
* botconfig.properties
  * channel=<channel name> (found in the channel URL at the end e.g. twich.tv/channel_name)
  * onlymods=false (set on true of only mods can use the bot)
  * usercooldownseconds=30 (set cooldown for commands for users)