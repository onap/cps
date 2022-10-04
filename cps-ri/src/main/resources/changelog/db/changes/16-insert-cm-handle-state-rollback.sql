delete from fragment where xpath ~* '^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state/lock-reason$';
delete from fragment where xpath ~* '^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state/datastores/operational$';
delete from fragment where xpath ~* '^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state/datastores$';
delete from fragment where xpath ~* '^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state$';