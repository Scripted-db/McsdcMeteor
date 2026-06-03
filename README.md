# McsdcMeteor

Meteor Client Addon for [mcsdc](https://mcsdc.online). hooks into the mcsdc api so you can find and join servers without leaving or tabbing out of the game.

you get a **MCSDC** button on the main menu. click it, log in with your api token and you're in.

needs **fabric + meteor client** (see releases for what version we're on).

## install

1. grab the `.jar` from [releases](https://codeberg.org/Syu/McsdcMeteor/releases)
2. drop it in your mods folder (same place as meteor client itself)
3. launch

if you're building from source: `./gradlew build`, jar lands in `build/libs/`.

## what can you do with this

hub from main menu / multiplayer. find servers, find player, friends, recent, ticket lookup.

search servers with filters, join or add to your list, hit **info** for flags/history/ticket id. **edit flags** if your token has write access (also on pause menu / disconnect screen). last search stays loaded so you dont have to re-fetch the same list.

friends tab shows who you are friends with and who's currently on a server, you can join your friends directly from there. **add** puts servers in multiplayer as `Mcsdc ...`, hub has **clear MCSDC servers**. searched a bunch? **next server** in pause/disconnect cycles the queue. `.ticketID` copies current server's ticket id.

## Disclaimer

- You need your own MCSDC API token. if you want an API Token, hop over to either one of the following discord servers:
    - [Xova Discord](https://discord.gg/VbwFzATvsm) OR [MCSDC Discord](https://discord.gg/TrsAk3Ay5T)

- This is a client for an existing API, not a griefing toolkit. What people do with this, is none of my business.