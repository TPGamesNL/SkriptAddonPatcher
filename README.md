# SkriptAddonPatcher
This tool can be used to update addons after the change of https://github.com/SkriptLang/Skript/pull/3924.

For more info, see https://github.com/SkriptLang/Skript/issues/4012.

**This is not a plugin.** For the plugin version, see https://github.com/TPGamesNL/RuntimeSkriptAddonPatcher

## How to use
Step 1: download SkriptAddonPatcher.jar from [the releases page](https://github.com/TPGamesNL/SkriptAddonPatcher/releases).

Step 2: copy the addons that should be converted to the same folder as SkriptAddonPatcher.jar

Step 3: open a command line or terminal window in that folder.

Step 4: run the command `java -jar SkriptAddonPatcher.jar myaddon.jar`, 
where `myaddon.jar` should be replaced with the file name of the addon you want to convert.

Step 5: if you see the message `0 class files were modified` (not a higher number than 0!) 
then the addon doesn't need have to be updated, and you don't have to use a new jar file. 

Step 6: if more then 0 class files were modified, copy the newly created file named `myaddon-CONVERTED.jar` 
to your server (where `myaddon` is the file name of the addon you converted) to use instead of the old version 
(you can rename it if you prefer that).
