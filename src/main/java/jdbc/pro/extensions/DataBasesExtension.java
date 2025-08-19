package jdbc.pro.extensions;

import jdbc.pro.data.DataBases;

public class DataBasesExtension implements SuiteExtension {
    @Override
    public void afterSuite() {
        DataBases.closeAllConnections();
    }
}
