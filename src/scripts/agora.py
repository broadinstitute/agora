################################################################################
# agora.py
# 
# A CLI providing convenient access to the Agora methods repository REST API.
#
# author: Bradt
# contact: dsde-engineering@broadinstitute.org
# 2015
# 
# Run python agora.py -h to get usage info
#
# NOTES:
# 
# Supports the push, get-by-reference, and list methods
# Supports both configurations and workflows
#
# Must find a way to programatically get an Oath token from the openAM server.
#   Currently, assumes you just grabbed the token and are passing it in as an
#   argument. Really hackish.
# 
################################################################################


from argparse import ArgumentParser
import os, sys, tempfile, subprocess
import getpass
import csv
import httplib
import urllib
import json

# Really ought to have this configured somewhere, but fine for now
agoraUrl="agora-ci.broadinstitute.org"  


def fail(message):
    print message
    sys.exit(1)

def get_endpoint(configurations, methods):
    if configurations:
        return "/configurations"
    elif methods:
        return "/methods"
    else:
        fail("No appropriate endpoint specified")

def get_push_namespace(namespace):
    if namespace:
        return namespace
    else:
        return getpass.getuser() 

def get_push_name(name, payloadFile):
    if name:
        return name
    else:
        base = os.path.basename(payloadFile)
        return os.path.splitext(base)[0]

def get_push_documentation(docsFile):
    if docsFile:
        print docsFile
        return read_entire_file(docsFile)
    else:
        return ""


# Read the entire contents of the payload file, removing leading/trailing whitespace.
# Performing no validation (methods repo api handles this)
def read_entire_file(inputFile):
    try:
        with open(inputFile) as myInput:
            return myInput.read().strip()
    except:
        print "Cannot read input file ", inputFile
        fail("File-read error")


# Bring up a text editor to solicit user input for methods post.
# First line of user text is synopsis, rest is documentation.
# Lines starting with # are ignored.
# Mimics git commit functionality
def get_user_synopsis():
    EDITOR = os.environ.get('EDITOR','vim')
    initial_message = "\n# Provide a 1-sentence synopsis (< 80 charactors) in your first line,\nSubsequent lines are ignored"
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
    return synopsis


# Performs the actual content POST to agora. Fails on non-201(created) responses.
def entity_post(authToken, endpoint, namespace, name, synopsis, documentation, entityType, payload, agoraUrl):
    requestUrl = endpoint
    addRequest = {"namespace": namespace, "name": name, "synopsis": synopsis, "documentation": documentation, "entityType": entityType, "payload": payload}
    requestBody = json.dumps(addRequest)
    conn = httplib.HTTPSConnection(agoraUrl)
    headers = {'Cookie': authToken, 'Content-type':  "application/json"}
    conn.request("POST", requestUrl, requestBody, headers=headers)
    r1 = conn.getresponse()
    data = r1.read()
    if r1.status != 201:
        print "ERROR! Unable to POST entity ", namespace, "/", name
        print "to ", endpoint
        print "Request:"
        print addRequest
        print "\nResponse:"
        print r1.status, r1.reason, data
        fail("[ERROR] Entity POST failed")
    return json.loads(data)

# Perform the actual GET using namespace, name, snapshotId
def entity_get(authToken, endpoint, namespace, name, snapshot_id):
    requestUrl = endpoint + "/" + namespace + "/" + name + "/" + str(snapshot_id)
    conn = httplib.HTTPSConnection(agoraUrl)
    headers = {'Cookie': authToken, 'Content-type':  "application/json"}
    conn.request("GET", requestUrl, headers=headers)
    r1 = conn.getresponse()
    data = r1.read()
    if r1.status != 200:
        print "ERROR! Unable to GET method ", namespace, "/", name, "/", snapshot_id
        print "from ", endpoint
        print "\nResponse:"
        print r1.status, r1.reason, data
        fail("[ERROR] Entity GET failed")
    return json.loads(data)

# Perform the actual GET to list entities filtered by query-string parameters
def entity_list(authToken, endpoint, queryString):
    requestUrl = endpoint + queryString
    conn = httplib.HTTPSConnection(agoraUrl)
    headers = {'Cookie': authToken, 'Content-type':  "application/json"}
    conn.request("GET", requestUrl, headers=headers)
    r1 = conn.getresponse()
    data = r1.read()
    if r1.status != 200:
        print "ERROR! Unable to GET list methods matching " + queryString
        print "from ", endpoint
        print "\nResponse:"
        print r1.status, r1.reason, data
        fail("[ERROR] List GET failed")
    return json.loads(data)

# Given program arguments, including a payload file, pushes content to agora
def push(args):
    endpoint = get_endpoint(args.configurations, args.methods)
    namespace = get_push_namespace(args.namespace)  
    name = get_push_name(args.name, args.PAYLOAD_FILE)
    documentation = get_push_documentation(args.docs)   
    payload = read_entire_file(args.PAYLOAD_FILE)
    synopsis = get_user_synopsis() 
    print entity_post(args.auth, endpoint, namespace, name, synopsis, documentation, args.entityType, payload, agoraUrl)

# Given program args namespace, name, id: pull a specific method
def pull(args):
    endpoint = get_endpoint(args.configurations, args.methods)
    print entity_get(args.auth, endpoint, args.namespace, args.name, args.snapshot_id)

# Given the program arguments, query the methods repository for a filtered list of methods
def search(args):
    endpoint = get_endpoint(args.configurations, args.methods)
    queryString = "?"
    if args.includedFields:
        for field in args.includedFields:
            queryString = queryString + "includedField=" + field + "&"
    if args.excludedFields:
        for field in args.excludedFields:
            queryString = queryString + "excludedField=" + field + "&"
    excludedFields = args.excludedFields
    args = args.__dict__
    trimmedArgs = {key: value for key, value in args.iteritems() if args[key] and key != 'func' and key != 'auth' and key != 'methods' and key != 'configurations' and key != 'excludedFields' and key != 'includedFields'}
    for key, value in trimmedArgs.iteritems():
        queryString = queryString + key + "=" + value + "&"
    queryString = queryString.rstrip("&")
    print entity_list(args['auth'], endpoint, queryString)

if __name__ == "__main__":
    # The main argument parser
    parser = ArgumentParser(description="CLI for accessing the AGORA methods repository. Currently only handles method push")
    
    # Core application arguments
    parser.add_argument('-a', '--auth', dest='auth', action='store', help='Oath token key=value pair for passing in request cookies')
    endpoint_group = parser.add_mutually_exclusive_group(required=True)
    endpoint_group.add_argument('-c', '--configurations', action='store_true', help='Operate on task-configurations, via the /configurations endpoint')
    endpoint_group.add_argument('-m', '--methods', action='store_true', help='Operate on tasks and workflows, via the /methods endpoint')    
    subparsers = parser.add_subparsers(help='Agora Methods Repository actions')
    
    # POST arguments
    push_parser = subparsers.add_parser('push', description='Push a method to the Agora Methods Repository', help='Push a method to the Agora Methods Repository')
    push_parser.add_argument('-s', '--namespace', dest='namespace', action='store', help='The namespace for method addition. Default value is your user login name')
    push_parser.add_argument('-n', '--name', dest='name', action='store', help='The method name to provide for method addition. Default is the name of the PAYLOAD_FILE.')
    push_parser.add_argument('-d', '--documentation', dest='docs', action='store', help='A file containing user documentation. Must be <10kb. May be plain text. Marking languages such as HTML or Github markdown are also supported')
    push_parser.add_argument('-t', '--entityType', dest='entityType', action='store', help='The type of the entities you are trying to get', choices=['Task', 'Workflow', 'Configuration'], default='Workflow')
    push_parser.add_argument('PAYLOAD_FILE', help='A file containing the payload. For configurations, JSON. For tasks + workflows, the method description in WDL')
    push_parser.set_defaults(func=push)
    
    # GET (namespace/name/id) arguments
    pull_parser = subparsers.add_parser('pull', description='Get a specific method snapshot from the Agora Methods Repository', help='Get a specific method snapshot from the Agora Methods Repository')
    pull_parser.add_argument('-s', '--namespace', dest='namespace', action='store', help='The namespace for the entity you are trying to get', required=True)
    pull_parser.add_argument('-n', '--name', dest='name', action='store', help='The name of the entity you are trying to get', required=True)
    pull_parser.add_argument('-i', '--snapshotId', dest='snapshotId', type=int, action='store', help='The snapshot-id of the entity you are trying to get', required=True)
    pull_parser.set_defaults(func=pull)
    
    # GET (query-paremeters) arguments
    search_parser = subparsers.add_parser('search', description='List methods in the Agora Methods Repository based on metadata', help='List methods in the Agora Methods Repository based on metadata')
    search_parser.add_argument('-f', '--includedFields', dest='includedFields', nargs='*', action='store', help='Any specific metadata fields you wish to be included in the response entities')
    search_parser.add_argument('-e', '--excludedFields', dest='excludedFields', nargs='*', action='store', help='Any specific metadata fields you wish to be excluded from the response entities')
    search_parser.add_argument('-s', '--namespace', dest='namespace', action='store', help='The namespace for the entities you are trying to get')
    search_parser.add_argument('-n', '--name', dest='name', action='store', help='The name of the entities you are trying to get')
    search_parser.add_argument('-i', '--snapshotId', dest='snapshotId', type=int, action='store', help='The snapshot-id of the entities you are trying to get')    
    search_parser.add_argument('-y', '--synopsis', dest='synopsis', action='store', help='The exact synopsis of the entities you are trying to get')
    search_parser.add_argument('-d', '--documentation', dest='docs', action='store', help='The exact documentation of the entities you are trying to get')
    search_parser.add_argument('-o', '--owner', dest='owner', action='store', help='The owner of the entities you are trying to get')
    search_parser.add_argument('-p', '--payload', dest='payload', action='store', help='The exact payload of the entities you are trying to get')
    search_parser.add_argument('-t', '--entityType', dest='entityType', action='store', help='The type of the entities you are trying to get',choices=['Task', 'Workflow', 'Configuration'])
    search_parser.set_defaults(func=search)

    # Call the appropriate function for the given subcommand, passing in the parsed program arguments
    args = parser.parse_args()
    args.func(args)

    



