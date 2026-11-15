# LifeSystem — Paper 1.21.11

## Settings
- 5 starting lives
- Lose one life per death
- At zero lives, the player is banned by username
- Lives shown in the tab list and below the player's name
- Lives saved in `plugins/LifeSystem/config.yml`

## Commands
- `/lives`
- `/lives set <player> <amount>` (online players; operator by default)
- `/lives reset <player>` (online players; operator by default)

## Build
GitHub Actions builds this project on every push. Open the **Actions** tab and download the
`LifeSystem` artifact after a successful run.

Requires Java 21 and Paper 1.21.11. Test on a backup server before installing on a live server.
