import json, os, getpass, sys
import xml.etree.ElementTree as ET
from xml.dom import minidom
from xml.sax.saxutils import unescape

SETUP_SCRIPT = "/lustre/expphy/work/hallb/hps/jeremym/datacat/setup.csh"
PYTHON_SCRIPT = "/lustre/expphy/work/hallb/hps/jeremym/datacat/hps-java/datacat/src/main/python/load_json.py"
DATACAT_CONFIG = "/lustre/expphy/work/hallb/hps/jeremym/datacat/default.cfg"

#TRACK_NAME = "simulation"
TRACK_NAME = "debug"

SEND_EMAIL = True

JOB_TIME = 4
#JOB_TIME = 24

if len(sys.argv) > 1:
    datafile = sys.argv[1]
else:
    raise Exception("Missing name of JSON input file.")
datafile = os.path.abspath(datafile)

def make_xml(datafile):

    json_data = open(datafile).read()
    data = json.loads(json_data)

    basename,ext = os.path.splitext(os.path.basename(datafile))

    req = ET.Element("Request")
    req_name = ET.SubElement(req, "Name")
    req_name.set("name", basename)

    prj = ET.SubElement(req, "Project")
    prj.set("name", "hps")

    trk = ET.SubElement(req, "Track")
    trk.set("name", TRACK_NAME)

    if SEND_EMAIL:
        email = ET.SubElement(req, "Email")
        email.set("email", getpass.getuser() + "@jlab.org")
        email.set("request", "true")
        email.set("job", "true")

    mem = ET.SubElement(req, "Memory")
    mem.set("space", "2000")
    mem.set("unit", "MB")
    limit = ET.SubElement(req, "TimeLimit")
    limit.set("time", str(JOB_TIME))
    limit.set("unit", "hours")
    os_elem = ET.SubElement(req, "OS")
    os_elem.set("name", "centos7")

    job = ET.SubElement(req, "Job")
    outfile = os.path.splitext(datafile)[0]
    stdout = ET.SubElement(job, "Stdout")
    stdout.set("dest", outfile + ".out")
    stderr = ET.SubElement(job, "Stderr")
    stderr.set("dest", outfile + ".err")
    infile = ET.SubElement(job, "Input")
    infile.set("src", DATACAT_CONFIG)
    infile.set("dest", os.path.basename(DATACAT_CONFIG))

    cmd = ET.SubElement(job, "Command")
    cmd_lines = []
    cmd_lines.append("<![CDATA[")
    cmd_lines.append("source %s" % SETUP_SCRIPT)
    cmd_lines.append("python %s %s" % (PYTHON_SCRIPT, datafile))
    cmd_lines.append("]]>")
    cmd_text = '\n'.join(cmd_lines)
    cmd_text = unescape(cmd_text)
    cmd.text = cmd_text

    pretty = unescape(minidom.parseString(ET.tostring(req)).toprettyxml(indent = "    "))
    xml_file = os.path.splitext(datafile)[0] + ".xml"
    with open(xml_file, "w") as f:
        f.write(pretty)
    print "Wrote Auger XML '%s'" % xml_file

make_xml(datafile)

