import os, argparse

class Crawler:

    def parse(self):
        parser = argparse.ArgumentParser("Crawl directories for files to enter in datacat")
        parser.add_argument("-e", "--extension", nargs="?", help="File extensions to include", action="append", default=[])
        parser.add_argument("-x", "--exclude", nargs="?", help="Pattern to exclude (path is excluded if it contains any exclude strings)", action="append", default=[])
        parser.add_argument("-i", "--include", nargs="?", help="Pattern to include (paths must contain all include strings)", action="append", default=[])
        parser.add_argument("-o", "--output", nargs=1, help="If set then write the output to the specified file, otherwise print the results")
        parser.add_argument("-v", "--verbose", action="store_true")
        parser.add_argument("directories", nargs="?", help="Directory to crawl", action="append", default=[])
        cl = parser.parse_args()

        self.directories = cl.directories
        if not (len(self.directories)):
            parser.print_help()
            raise Exception("At least one directory is required.")

        excludes = cl.exclude
        includes = cl.include
        extensions = cl.extension
        self.verbose = cl.verbose

        self.output = cl.output[0]

        self.filters = []
        if len(extensions):
            if self.verbose:
                print "Extensions: " + str(extensions)
            self.filters.append(FileExtFilter(extensions)) 
        if len(excludes):
            if self.verbose:
                print "Excludes: " + str(excludes)
            self.filters.append(ExcludeFilter(excludes))
        if len(includes):
            if self.verbose:
                print "Includes: " + str(includes)
            self.filters.append(IncludeFilter(includes))

    def run(self):
        self.results = []
        for dirname in self.directories:
            if self.verbose:
                print "Crawling '%s'" % dirname
            for directory, dirnames, filenames in os.walk(dirname):
                for filename in filenames:
                    path = os.path.join(directory, filename)
                    if self.check_file(path):
                        self.results.append(path)
                    
    def check_file(self, filename):
        for f in self.filters:
            if not f.check_file(filename):
                return False
        return True    

    def print_results(self):
        for r in self.results:
            print r
  
    def write_results(self):
        with open(self.output, 'w') as f:
            for r in self.results:
                f.write(r + '\n')
        if self.verbose:
            print "Wrote output to '%s'" % self.output
    
class Filter:

    def check_file(self, filename):
        return True

class FileExtFilter(Filter):

    def __init__(self, extensions):
        self.extensions = extensions

    def check_file(self, filename):
        x,ext = os.path.splitext(filename)
        return ext in self.extensions

class ExcludeFilter(Filter):

    def __init__(self, excludes):
        self.excludes = excludes
 
    def check_file(self, filename):
        for exclude in self.excludes:
            if exclude in filename:
                return False
        return True

class IncludeFilter(Filter):

    def __init__(self, includes):
        self.includes = includes

    def check_file(self, filename):
        for include in self.includes:
            if not include in filename:
                return False
        return True

if __name__ == "__main__":
    c = Crawler()
    c.parse()
    c.run()
    if c.output:
        c.write_results()
    else:
        c.print_results()    
