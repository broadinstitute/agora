################################################################################
# agora.py
# 
# A CLI providing convenient access to the Agora methods repository REST API.
#
# author: Bradt
# contact: dsde-engineering@broadinstitute.org
# 2015
# 
# NOTES:
# This is a very rough initial implementation. Needs a lot of work.
#
# Currently just supports method push. Ulitmately we will want method fetch, and
#   task-configuration push/fetch.
#
# Must find a way to programatically get an Oath token from the openAM server.
#   Currently, assumes you just grabbed the token and are passing it in as an
#   argument. Really hackish.
#
# Gets default parameters from a .agoraconfig file, assumed to be in the same
#   directory. Overrides some of them from the command line. I'm not really
#   happy with the argument parsing/validation here. I would love to get input
#   from a stronger python programmer. In particular, can we set up different args
#   args to support multiple 'modes', like FETCH / PUSH.
# 
################################################################################


from argparse import ArgumentParser
import os, sys, tempfile, subprocess
import csv
import httplib
import urllib
import json

ownerConfig = "owner="
namespaceConfig = "namespace="
urlConfig = "agora_url="

def fail(message):
    print message
    sys.exit(1)


# Read defualt values out of the configuration file. In particular, owner.
# TODO - is there a smarter way to do this?
def read_agoraconfig():
    namespace = owner = agoraUrl = False
    with open(".agoraconfig") as configFile:
        configLines = configFile.readlines()
        for line in configLines:
            if line.startswith(ownerConfig):
                owner = line[len(ownerConfig):].strip()
            elif line.startswith(namespaceConfig):
                namespace = line[len(namespaceConfig):].strip()
            elif line.startswith(urlConfig):
                agoraUrl = line[len(urlConfig):].strip()
    return (namespace, owner, agoraUrl)


# Read the entire contents of the payload file, removing leading/trailing whitespace.
# Performing no validation (methods repo api handles this)
def read_payload_file(payloadFile):
    with open(payloadFile) as payFile:
        payload = payFile.read().strip()
    return payload    


# Bring up a text editor to solicit user input for methods post.
# First line of user text is synopsis, rest is documentation.
# Lines starting with # are ignored.
# Mimics git commit functionality
def get_user_documentation():
    EDITOR = os.environ.get('EDITOR','vim')
    initial_message = "\n# Provide a 1-sentence synopsis (< 80 charactors) in your first line,\n# followed by a more extensive body of documentation. Lines starting with # are ignored."
    lines = []
    with tempfile.NamedTemporaryFile(suffix=".tmp") as tmpfile:
        tmpfile.write(initial_message)
        tmpfile.flush()
        subprocess.call([EDITOR, tmpfile.name])
        with open(tmpfile.name) as userinPut:
            lines = userinPut.readlines()
    
    synopsis = lines[0].strip()
    if len(synopsis) > 80:
        fail("[ERROR] Synopsis must be < 80 charactors")
    documentation = ""
    #TODO - use a list comprehension here.
    for line in lines[1:]:
        if not line.startswith("#"):
            documentation = documentation + line
    documentation = documentation.strip()
    return (synopsis, documentation)


# Posts the method to agora /methods endpoint. Fails on non-200 responses.
def method_post(namespace, name, synopsis, documentation, owner, payload, agoraUrl):
    requestUrl = "/methods"
    addRequest = {"namespace": namespace, "name": name, "synopsis": synopsis, "documentation": documentation, "owner": owner, "payload": payload}
    requestBody = json.dumps(addRequest)
    conn = httplib.HTTPSConnection(agoraUrl)
    headers = {'Cookie': auth, 'Content-type':  "application/json"}
    conn.request("POST", requestUrl, requestBody, headers=headers)
    r1 = conn.getresponse()
    data = r1.read()
    if r1.status != 200:
        print "ERROR! Unable to POST method ", namespace, "/", name
        print "Request:"
        print addRequest
        print "\nResponse:"
        print r1.status, r1.reason
        fail("[ERROR] Method post failed")
    return json.loads(data)


# Given program arguments, including a payload WDL file, pushes a method to agora
def push_method(namespace, name, owner, payloadFile, agoraUrl):
    payload = read_payload_file(payloadFile)
    (synopsis, documentation) = get_user_documentation()
    print method_post(namespace, name, synopsis, documentation, owner, payload, agoraUrl)


if __name__ == "__main__":
    parser = ArgumentParser(description="CLI for accessing the AGORA methods store. Currently only handles method add")
    parser.add_argument('-a', '--auth', dest='auth', action='store', help='Oath token key=value pair for passing in request cookies')
    parser.add_argument('-s', '--namespace', dest='namespace', action='store', help='The namespace for method addition. Overrides the default in .agoraconfig')
    parser.add_argument('-n', '--name', dest='name', action='store', help='The method name to provide for method addition. Default is the name of the INPUT file.')
    parser.add_argument('INPUT', help='A file containing the method description in WDL format')
    args = parser.parse_args()

    # Get default parameters from .agoraconfig file
    (namespace, owner, agoraUrl) = read_agoraconfig()

    # Capture user-supplied arguments. Override defaults if supplied.
    # TODO - We should really be logging into the openAM server and getting the token programatically.
    #        Supplying the auth token from command line is clunky, and unusable outside of dev.
    auth = args.auth
    name = args.name    
    payloadFile = args.INPUT

    # validate parameters. TODO - this is admittedly ugly. Can it be simplified?
    if not owner:
        fail("[ERROR] owner not defined. Please updated .agoraconfig file.")
    if not agoraUrl:
        agoraUrl = "agora-ci.broadinstitute.org"
        print "[INFO] agoraUrl not defined in .agoraconfig file. Defaulting to", agoraUrl
    if not namespace:
        if not args.namespace:
            fail("[ERROR] namespace not defined. Please updated .agoraconfig file or supply -s argument.")
        else:
            namespace = args.namespace
    if not name:
        base = os.path.basename(payloadFile)
        name = os.path.splitext(base)[0]     

    print namespace, name, owner, payloadFile, agoraUrl

    # Perform the actual method upload
    push_method(namespace, name, owner, payloadFile, agoraUrl)

    



