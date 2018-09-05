#!/usr/bin/env python
import sys
import argparse
import ROOT

def print_nodes(node,depth,max_depth,num):
    """Print the current node, then find all the nodes in the tree below and recurse."""

    name = node.GetName()
    mat  = node.GetMatrix()
    pos = mat.GetTranslation()
    space="    "*depth
    print("{:3d} - ".format(num)+space+"{:25s} [{:5.2f},{:5.2f},{:5.2f}]".format(name,pos[0],pos[1],pos[2]))

    if depth<max_depth:
        for i in range(node.GetNdaughters()):
            next_node = node.GetDaughter(i)
            print_nodes(next_node,depth+1,max_depth,i)

def main(argv):
    parser = argparse.ArgumentParser(
                description="""GDML Geometry walker using ROOT.""",
                epilog="""For more information, or errors, please email: maurik@physics.unh.edu """)
    parser.add_argument('file',nargs=1,type=str,help='GDML file to open an walk.')
    parser.add_argument('-d','--debug',action="count",help='Increase debug level by one.')

    args = parser.parse_args(argv)

    print("Opening {} with TGeoManager.Import ".format(args.file[0]))
    ROOT.TGeoManager.Import(args.file[0])
    g = ROOT.gGeoManager
    top = g.GetTopNode()

    print_nodes(top,0,10,0)


if __name__ == "__main__":
    main(sys.argv[1:])
