DELETE FROM fragment WHERE xpath ~* '^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state/lock-reason$';
DELETE FROM fragment WHERE xpath ~* '^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state/datastores/operational$';
DELETE FROM fragment WHERE xpath ~* '^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state/datastores$';
DELETE FROM fragment WHERE xpath ~* '^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state$';