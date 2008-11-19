def outputKlassTable(linesize=4):
    klasses = []
    for line in open('training-classlist','r'):
        if not (line == '' or line == '?'):
            klasses.append(line.strip().replace('_',' '))
    klasses.sort()
    for n, item in enumerate(klasses):
        if (n+1) % 4 == 0:
            print item, '\\\\'
        else:
            print item, '&',

if __name__ == '__main__':
    outputKlassTable()