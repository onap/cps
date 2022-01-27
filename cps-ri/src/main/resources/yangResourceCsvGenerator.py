import csv
import hashlib
import sys

revision = sys.argv[1]

with open('../../../../cps-service/src/test/resources/dmi-registry.yang') as content:
    dmiRegistry = content.read()

checksum = hashlib.sha256().hexdigest()

# open the file in the write mode
with open('changelog/db/changes/data/dmi/generated_yang_resource.csv', 'w', newline='') as file:
    writer = csv.writer(file, delimiter='|')
    writer.writerow(["name", "content", "checksum"])
    writer.writerow(["dmi-registry@" + revision + ".yang", dmiRegistry, checksum])