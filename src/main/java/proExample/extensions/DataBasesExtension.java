package proExample.extensions;

import proExample.data.DataBases;

public class DataBasesExtension implements SuiteExtension {
    @Override
    public void afterSuite() {
        DataBases.closeAllConnections();
    }
}
