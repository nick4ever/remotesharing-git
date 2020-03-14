import os
import shutil

#backUpPath = os.path.join("M:\\", "Backups")
#pattern = "BACKUP"
backUpPath = "/home/nick4ever"
pattern = "Downloads"

for d in os.listdir(backUpPath):
    p = os.path.join(backUpPath, d)
    if os.path.isdir(p) and d.startswith(pattern):
        print(p)
#        shutil.rmtree(p)
