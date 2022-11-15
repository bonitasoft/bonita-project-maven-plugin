import java.io.*

def daoOutputFolder = new File( basedir, 'dao-client/target/generated-sources/java');
def daoPackageFolder = daoOutputFolder.toPath().resolve('com').resolve('company').resolve('model').toFile()
def daoFileNames = daoPackageFolder.listFiles().collect { it.name }
assert !daoFileNames.contains('Quotation.java') : 'Quotation.java should not be generated in the output folder'
assert !daoFileNames.contains('QuotationDAO.java') : 'QuotationDAO.java should not be generated in the output folder'
assert !daoFileNames.contains('Supplier.java') : 'Supplier.java should not be generated in the output folder'
assert !daoFileNames.contains('SupplierDAO.java') : 'SupplierDAO.java should not be generated in the output folder'
assert !daoFileNames.contains('Request.java') : 'Request.java should not be generated in the output folder'
assert !daoFileNames.contains('RequestDAO.java') : 'RequestDAO.java should not be generated in the output folder'
assert daoFileNames.contains('QuotationDAOImpl.java') : 'QuotationDAOImpl.java is missing from the generated output folder'
assert daoFileNames.contains('SupplierDAOImpl.java') : 'SupplierDAOImpl.java is missing from the generated output folder'
assert daoFileNames.contains('RequestDAOImpl.java') : 'RequestDAOImpl.java is missing from the generated output folder'

def modelOutputFolder = new File( basedir, 'model/target/generated-sources/java');
def modelPackageFolder = modelOutputFolder.toPath().resolve('com').resolve('company').resolve('model').toFile()
def modelFileNames = modelPackageFolder.listFiles().collect { it.name }
assert modelFileNames.contains('Quotation.java') : 'Quotation.java is missing from the generated output folder'
assert modelFileNames.contains('QuotationDAO.java') : 'QuotationDAO.java is missing from the generated output folder'
assert modelFileNames.contains('Supplier.java') : 'Supplier.java is missing from the generated output folder'
assert modelFileNames.contains('SupplierDAO.java') : 'SupplierDAO.java is missing from the generated output folder'
assert modelFileNames.contains('Request.java') : 'Request.java is missing from the generated output folder'
assert modelFileNames.contains('RequestDAO.java') : 'RequestDAO.java is missing from the generated output folder'
assert !modelFileNames.contains('RequestDAOImpl.java') : 'RequestDAOImpl.java should not be generated in the output folder'
