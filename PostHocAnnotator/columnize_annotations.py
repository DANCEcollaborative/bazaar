import csv
import os
import sys

annotations = set()

def columnize_all(path, out_path):
    for in_file in paths:
        anonymize(os.path.join(path, in_file), os.path.join(out_path, in_file))
        
        
def columnize(in_file, out_file):
    global annotations
    with open(in_file, "rU") as rf:
        reader = csv.DictReader(rf)
        for row in reader: 
            notes=row['ANNOTATIONS'].split("+")
            annotations = annotations | set(notes)
            
    with open(in_file, "rU") as rf:
        with open(out_file, 'w') as wf:
            reader = csv.DictReader(rf)
            note_names = list(annotations)
            note_names.remove("")
            output_annotations = ["DATE","TIME","AUTHOR","TEXT","NOTE","NONE"] + note_names
            writer = csv.DictWriter(wf,output_annotations)
            writer.writeheader()
            for row in reader:        
                notes=row['ANNOTATIONS'].split("+")
                for note in annotations:
                    row[note] = min(notes.count(note),1)
                     
                row["NONE"] = row[""]
                del row[""]
                del row["ANNOTATIONS"]
                
                if not row["NOTE"]:
                    row["NOTE"] = ""
                    
                print row
                
                writer.writerow(row)
        

path = sys.argv[1]
out_path = sys.argv[2]

columnize(path, out_path)
