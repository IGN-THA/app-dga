from docx2pdf import convert
import sys

#print("\nName of Python script:", sys.argv[1]) 
#print("\nName of Python script:", sys.argv[2]) 

#convert("C:\WorkFolder\DocProcess\RenderedFilePath", "C:\WorkFolder\DocProcess\PrintFolder")

convert(sys.argv[1], sys.argv[2])