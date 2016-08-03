{
TGeoManager *geo=new TGeoManager("geo","HPS");
TGeoManager *g2=geo->Import("lcdd_cleaned.gdml");
g2->GetMasterVolume()->Draw("ogl");
}
